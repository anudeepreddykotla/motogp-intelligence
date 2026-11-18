package com.motogp.intelligence.ingestion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReferenceDataIngestionService {
    private static final List<ReferenceEntityType> INGESTION_ORDER = List.of(
            ReferenceEntityType.SEASON,
            ReferenceEntityType.MANUFACTURER,
            ReferenceEntityType.CIRCUIT,
            ReferenceEntityType.TEAM,
            ReferenceEntityType.RIDER,
            ReferenceEntityType.EVENT);

    private final ProviderAdapterRegistry providers;
    private final IdentityMappingService identityMappings;
    private final ReferenceDataStore store;
    private final IngestionRunStore ingestionRuns;
    private final Clock clock;

    @Autowired
    public ReferenceDataIngestionService(ProviderAdapterRegistry providers,
                                         IdentityMappingService identityMappings,
                                         ReferenceDataStore store, IngestionRunStore ingestionRuns) {
        this(providers, identityMappings, store, ingestionRuns, Clock.systemUTC());
    }

    ReferenceDataIngestionService(ProviderAdapterRegistry providers,
                                  IdentityMappingService identityMappings,
                                  ReferenceDataStore store, IngestionRunStore ingestionRuns,
                                  Clock clock) {
        this.providers = providers;
        this.identityMappings = identityMappings;
        this.store = store;
        this.ingestionRuns = ingestionRuns;
        this.clock = clock;
    }

    @Transactional
    public ReferenceIngestionResult ingest(String providerCode) {
        HistoricalDataProvider provider = providers.require(providerCode);
        Map<ReferenceEntityType, List<ReferenceData>> records = loadAndValidate(provider);
        Instant startedAt = Instant.now(clock);
        UUID jobId = ingestionRuns.ensureReferenceJob(providerCode);
        UUID attemptId = ingestionRuns.startAttempt(jobId, startedAt);
        Map<String, UUID> idsByExternalId = new HashMap<>();
        Map<ReferenceEntityType, Integer> writtenByType = new EnumMap<>(ReferenceEntityType.class);
        int recordsWritten = 0;

        for (ReferenceEntityType type : INGESTION_ORDER) {
            for (ReferenceData record : records.getOrDefault(type, List.of())) {
                boolean created;
                UUID id;
                switch (record) {
                    case SeasonReference season -> {
                        ReferenceDataStore.SeasonRow existing = store.findSeason(season.year(), season.championship()).orElse(null);
                        if (existing != null) {
                            requireSame(existing.year() == season.year()
                                            && existing.championship().equals(season.championship()),
                                    type, season, "natural key resolved to different season values");
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertSeason(season);
                            created = true;
                        }
                    }
                    case ManufacturerReference manufacturer -> {
                        ReferenceDataStore.ManufacturerRow existing = store.findManufacturer(manufacturer.name()).orElse(null);
                        if (existing != null) {
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertManufacturer(manufacturer);
                            created = true;
                        }
                    }
                    case CircuitReference circuit -> {
                        ReferenceDataStore.CircuitRow existing = store.findCircuit(circuit.name(), circuit.countryCode()).orElse(null);
                        if (existing != null) {
                            requireSame(sameCircuit(existing, circuit), type, circuit,
                                    "natural key resolved to different circuit values");
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertCircuit(circuit);
                            created = true;
                        }
                    }
                    case TeamReference team -> {
                        ReferenceDataStore.TeamRow existing = store.findTeam(team.name(), team.countryCode()).orElse(null);
                        if (existing != null) {
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertTeam(team);
                            created = true;
                        }
                    }
                    case RiderReference rider -> {
                        ReferenceDataStore.RiderRow existing = store.findRider(rider.displayName()).orElse(null);
                        if (existing != null) {
                            requireSame(sameRider(existing, rider), type, rider,
                                    "natural key resolved to different rider values");
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertRider(rider);
                            created = true;
                        }
                    }
                    case EventReference event -> {
                        UUID seasonId = requireDependency(providerCode, idsByExternalId, event,
                                event.seasonExternalId(), ReferenceEntityType.SEASON);
                        UUID circuitId = requireDependency(providerCode, idsByExternalId, event,
                                event.circuitExternalId(), ReferenceEntityType.CIRCUIT);
                        ReferenceDataStore.EventRow existing = store.findEvent(seasonId, event.name()).orElse(null);
                        if (existing != null) {
                            requireSame(sameEvent(existing, event, seasonId, circuitId), type, event,
                                    "natural key resolved to different event values");
                            id = existing.id();
                            created = false;
                        } else {
                            id = store.insertEvent(event, seasonId, circuitId);
                            created = true;
                        }
                    }
                    default -> throw new InvalidReferenceDataException(providerCode, type, record.externalId(),
                            "unsupported reference type");
                }

                IdentityMappingResult mapping = identityMappings.map(providerCode, type, record.externalId(), id);
                store.attachPrimarySourceRecord(type.tableName(), id, mapping.sourceRecordId());
                idsByExternalId.put(type.name() + ":" + record.externalId(), id);
                if (created) {
                    recordsWritten++;
                    writtenByType.merge(type, 1, Integer::sum);
                }
            }
        }

        ingestionRuns.finishAttempt(attemptId, "SUCCEEDED",
                records.values().stream().mapToInt(List::size).sum(), recordsWritten, Instant.now(clock));
        return new ReferenceIngestionResult(providerCode,
                records.values().stream().mapToInt(List::size).sum(), recordsWritten,
                Map.copyOf(writtenByType));
    }

    private Map<ReferenceEntityType, List<ReferenceData>> loadAndValidate(HistoricalDataProvider provider) {
        Map<ReferenceEntityType, List<ReferenceData>> records = new EnumMap<>(ReferenceEntityType.class);
        Set<String> externalKeys = new HashSet<>();
        for (ReferenceEntityType type : INGESTION_ORDER) {
            List<ReferenceData> loaded = provider.findReferenceData(new ReferenceQuery(type, null));
            for (ReferenceData record : loaded) {
                if (record == null || record.entityType() != type) {
                    throw new InvalidReferenceDataException(provider.code(), type, "unknown", "record type does not match query");
                }
                validateCommon(provider.code(), record);
                if (!externalKeys.add(type.name() + ":" + record.externalId())) {
                    throw new DuplicateReferenceException(provider.code(), type, record.externalId());
                }
                validateSpecific(provider.code(), record);
            }
            records.put(type, List.copyOf(loaded));
        }
        return records;
    }

    private void validateCommon(String providerCode, ReferenceData record) {
        if (blank(record.externalId()) || blank(record.sourceVersion()) || blank(record.displayName())) {
            throw new InvalidReferenceDataException(providerCode, record.entityType(), record.externalId(),
                    "external ID, source version, and display name are required");
        }
    }

    private void validateSpecific(String providerCode, ReferenceData record) {
        switch (record) {
            case SeasonReference season -> {
                if (season.year() < 1949 || season.year() > 2100 || blank(season.championship())) {
                    invalid(providerCode, record, "season year or championship is invalid");
                }
            }
            case ManufacturerReference manufacturer -> requireName(providerCode, manufacturer, manufacturer.name());
            case TeamReference team -> {
                requireName(providerCode, team, team.name());
                validateCountry(providerCode, team, team.countryCode());
            }
            case RiderReference rider -> {
                requireName(providerCode, rider, rider.displayName());
                validateCountry(providerCode, rider, rider.nationalityCode());
            }
            case CircuitReference circuit -> {
                requireName(providerCode, circuit, circuit.name());
                validateCountry(providerCode, circuit, circuit.countryCode());
                validateTimezone(providerCode, circuit, circuit.timezone());
                validateCoordinates(providerCode, circuit, circuit.latitude(), circuit.longitude());
            }
            case EventReference event -> {
                requireName(providerCode, event, event.name());
                if (event.roundNumber() <= 0 || blank(event.seasonExternalId()) || blank(event.circuitExternalId())) {
                    invalid(providerCode, event, "round number and season/circuit dependencies are required");
                }
                validateTimezone(providerCode, event, event.timezone());
            }
        }
    }

    private void requireName(String providerCode, ReferenceData record, String value) {
        if (blank(value)) {
            invalid(providerCode, record, "name is required");
        }
    }

    private void validateCountry(String providerCode, ReferenceData record, String countryCode) {
        if (countryCode != null && !countryCode.matches("[A-Z]{2}")) {
            invalid(providerCode, record, "country code must be two uppercase letters");
        }
    }

    private void validateTimezone(String providerCode, ReferenceData record, String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (RuntimeException exception) {
            invalid(providerCode, record, "timezone is invalid");
        }
    }

    private void validateCoordinates(String providerCode, ReferenceData record, BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null && (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0)) {
            invalid(providerCode, record, "latitude is outside the valid range");
        }
        if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0)) {
            invalid(providerCode, record, "longitude is outside the valid range");
        }
    }

    private UUID requireDependency(String providerCode, Map<String, UUID> ids, EventReference event, String externalId,
                                   ReferenceEntityType type) {
        UUID id = ids.get(type.name() + ":" + externalId);
        if (id == null) {
            throw new InvalidReferenceDataException(providerCode, type, externalId,
                    "event " + event.externalId() + " references a missing dependency");
        }
        return id;
    }

    private boolean sameCircuit(ReferenceDataStore.CircuitRow row, CircuitReference data) {
        return row.name().equals(data.name()) && equal(row.countryCode(), data.countryCode())
                && row.timezone().equals(data.timezone()) && equalDecimal(row.latitude(), data.latitude())
                && equalDecimal(row.longitude(), data.longitude());
    }

    private boolean sameRider(ReferenceDataStore.RiderRow row, RiderReference data) {
        return row.displayName().equals(data.displayName()) && equal(row.givenName(), data.givenName())
                && equal(row.familyName(), data.familyName()) && equal(row.nationalityCode(), data.nationalityCode());
    }

    private boolean sameEvent(ReferenceDataStore.EventRow row, EventReference data, UUID seasonId, UUID circuitId) {
        return row.seasonId().equals(seasonId) && row.circuitId().equals(circuitId) && row.name().equals(data.name())
                && row.roundNumber() == data.roundNumber() && equal(row.scheduledStartAt(), data.scheduledStartAt())
                && row.timezone().equals(data.timezone());
    }

    private void requireSame(boolean same, ReferenceEntityType type, ReferenceData record, String message) {
        if (!same) {
            throw new ReferenceDataConflictException(type, record.externalId(), message);
        }
    }

    private void invalid(String providerCode, ReferenceData record, String message) {
        throw new InvalidReferenceDataException(providerCode, record.entityType(), record.externalId(), message);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean equalDecimal(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }
}
