package com.motogp.intelligence.ingestion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RaceDataIngestionService {
    private final ProviderAdapterRegistry providers;
    private final IdentityMappingStore identityStore;
    private final IngestionRunStore ingestionRuns;
    private final RaceDataPersistenceService persistence;
    private final Clock clock;

    @Autowired
    public RaceDataIngestionService(ProviderAdapterRegistry providers, IdentityMappingStore identityStore,
                                    IngestionRunStore ingestionRuns, RaceDataPersistenceService persistence) {
        this(providers, identityStore, ingestionRuns, persistence, Clock.systemUTC());
    }

    RaceDataIngestionService(ProviderAdapterRegistry providers, IdentityMappingStore identityStore,
                             IngestionRunStore ingestionRuns, RaceDataPersistenceService persistence, Clock clock) {
        this.providers = providers;
        this.identityStore = identityStore;
        this.ingestionRuns = ingestionRuns;
        this.persistence = persistence;
        this.clock = clock;
    }

    public RaceDataIngestionResult ingest(String providerCode) {
        HistoricalDataProvider provider = providers.require(providerCode);
        UUID attemptId = null;
        int recordsSeen = 0;
        try {
            UUID jobId = ingestionRuns.ensureHistoricalJob(providerCode);
            attemptId = ingestionRuns.startAttempt(jobId, Instant.now(clock));
            List<SessionResultReference> results = load(provider, RaceDataType.SESSION_RESULT, SessionResultReference.class);
            List<LapReference> laps = load(provider, RaceDataType.RIDER_LAP, LapReference.class);
            List<RacePositionReference> positions = load(provider, RaceDataType.RACE_POSITION, RacePositionReference.class);
            recordsSeen = results.size() + laps.size() + positions.size();
            validate(providerCode, results, laps, positions);

            UUID providerId = identityStore.providerId(providerCode);
            Map<String, UUID> sessions = resolve(providerCode, providerId, ReferenceEntityType.SESSION,
                    results.stream().map(SessionResultReference::sessionExternalId).toList());
            Map<String, UUID> riders = resolve(providerCode, providerId, ReferenceEntityType.RIDER,
                    results.stream().map(SessionResultReference::riderExternalId).toList());
            Map<String, UUID> teams = resolveOptional(providerCode, providerId, ReferenceEntityType.TEAM,
                    results.stream().map(SessionResultReference::teamExternalId).toList());
            Map<String, UUID> manufacturers = resolveOptional(providerCode, providerId, ReferenceEntityType.MANUFACTURER,
                    results.stream().map(SessionResultReference::manufacturerExternalId).toList());

            RaceDataIngestionResult result = persistence.persist(providerCode, results, laps, positions,
                    sessions, riders, teams, manufacturers);
            ingestionRuns.finishAttempt(attemptId, "SUCCEEDED", recordsSeen, result.recordsWritten(), Instant.now(clock));
            return result;
        } catch (RuntimeException exception) {
            if (attemptId != null) {
                ingestionRuns.finishFailedAttempt(attemptId, recordsSeen, summarize(exception), Instant.now(clock));
            }
            throw exception;
        }
    }

    private <T extends RaceData> List<T> load(HistoricalDataProvider provider, RaceDataType type, Class<T> expectedType) {
        return provider.findRaceData(new RaceDataQuery(type, null)).stream()
                .map(record -> {
                    if (!expectedType.isInstance(record)) {
                        throw new InvalidRaceDataException(provider.code(), record.entityType(), record.externalId(),
                                "provider returned an unexpected race-data type");
                    }
                    return expectedType.cast(record);
                }).toList();
    }

    private void validate(String providerCode, List<SessionResultReference> results,
                          List<LapReference> laps, List<RacePositionReference> positions) {
        Set<String> resultExternalIds = new HashSet<>();
        Set<ClassificationKey> resultKeys = new HashSet<>();
        for (SessionResultReference result : results) {
            common(providerCode, result);
            duplicate(resultExternalIds, providerCode, result);
            require(providerCode, result, result.sessionExternalId(), "session dependency is required");
            require(providerCode, result, result.riderExternalId(), "rider dependency is required");
            require(providerCode, result, result.classificationStatus(), "classification status is required");
            if (!List.of("CLASSIFIED", "DNF", "DNS", "DSQ", "OTHER").contains(result.classificationStatus())
                    || positiveOrNull(result.gridPosition()) == false || positiveOrNull(result.classificationPosition()) == false
                    || nonNegativeOrNull(result.lapsCompleted()) == false || positiveOrNull(result.totalTimeMs()) == false
                    || result.points() != null && result.points().signum() < 0) {
                throw invalid(providerCode, result, "classification fields are invalid");
            }
            ClassificationKey key = new ClassificationKey(result.sessionExternalId(), result.riderExternalId());
            if (!resultKeys.add(key)) {
                throw invalid(providerCode, result, "duplicate session result logical key " + key);
            }
        }
        Set<String> lapExternalIds = new HashSet<>();
        Set<LapKey> lapKeys = new HashSet<>();
        for (LapReference lap : laps) {
            common(providerCode, lap);
            duplicate(lapExternalIds, providerCode, lap);
            requireResult(providerCode, lap, resultKeys);
            if (lap.lapNumber() <= 0 || positiveOrNull(lap.lapTimeMs()) == false) {
                throw invalid(providerCode, lap, "lap number or lap time is invalid");
            }
            LapKey key = new LapKey(lap.sessionExternalId(), lap.riderExternalId(), lap.lapNumber());
            if (!lapKeys.add(key)) {
                throw invalid(providerCode, lap, "duplicate rider lap logical key " + key);
            }
        }
        Set<String> positionExternalIds = new HashSet<>();
        Set<LapKey> positionKeys = new HashSet<>();
        for (RacePositionReference position : positions) {
            common(providerCode, position);
            duplicate(positionExternalIds, providerCode, position);
            requireResult(providerCode, position, resultKeys);
            if (position.lapNumber() < 0 || positiveOrNull(position.position()) == false || position.observedAt() == null) {
                throw invalid(providerCode, position, "position snapshot fields are invalid");
            }
            LapKey key = new LapKey(position.sessionExternalId(), position.riderExternalId(), position.lapNumber());
            if (!positionKeys.add(key)) {
                throw invalid(providerCode, position, "duplicate race position logical key " + key);
            }
        }
    }

    private Map<String, UUID> resolve(String providerCode, UUID providerId, ReferenceEntityType type,
                                      List<String> externalIds) {
        Map<String, UUID> result = new HashMap<>();
        for (String externalId : externalIds.stream().distinct().toList()) {
            IdentityMappingStore.ExistingMapping mapping = identityStore.findMapping(providerId, type, externalId);
            if (mapping == null) {
                throw new InvalidRaceDataException(providerCode, type, externalId,
                        "reference dependency has not been ingested or mapped");
            }
            result.put(externalId, mapping.internalId());
        }
        return result;
    }

    private Map<String, UUID> resolveOptional(String providerCode, UUID providerId, ReferenceEntityType type,
                                              List<String> externalIds) {
        return resolve(providerCode, providerId, type, externalIds.stream().filter(id -> id != null).toList());
    }

    private void common(String providerCode, RaceData record) {
        if (record.externalId() == null || record.externalId().isBlank()
                || record.sourceVersion() == null || record.sourceVersion().isBlank()) {
            throw invalid(providerCode, record, "external ID and source version are required");
        }
    }

    private void duplicate(Set<String> ids, String providerCode, RaceData record) {
        if (!ids.add(record.externalId())) {
            throw new InvalidRaceDataException(providerCode, record.entityType(), record.externalId(),
                    "duplicate provider external identifier");
        }
    }

    private void require(String providerCode, RaceData record, String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRaceDataException(providerCode, record.entityType(), record.externalId(), message);
        }
    }

    private void requireResult(String providerCode, RaceData record, Set<ClassificationKey> resultKeys) {
        ClassificationKey key = switch (record) {
            case LapReference lap -> new ClassificationKey(lap.sessionExternalId(), lap.riderExternalId());
            case RacePositionReference position -> new ClassificationKey(position.sessionExternalId(), position.riderExternalId());
            default -> null;
        };
        if (!resultKeys.contains(key)) {
            throw new InvalidRaceDataException(providerCode, record.entityType(), record.externalId(),
                    "no matching session result exists");
        }
    }

    private boolean positiveOrNull(Number value) {
        return value == null || value.doubleValue() > 0;
    }

    private boolean nonNegativeOrNull(Number value) {
        return value == null || value.doubleValue() >= 0;
    }

    private InvalidRaceDataException invalid(String providerCode, RaceData record, String message) {
        return new InvalidRaceDataException(providerCode, record.entityType(), record.externalId(), message);
    }

    private String summarize(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private record ClassificationKey(String sessionExternalId, String riderExternalId) {
    }

    private record LapKey(String sessionExternalId, String riderExternalId, short lapNumber) {
    }
}
