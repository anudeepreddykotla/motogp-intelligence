package com.motogp.intelligence.ingestion;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Explicitly reconciles the local synthetic fixture before reusing the normal
 * reference and race ingestion pipeline. This operation is intentionally
 * limited to the fixture provider and is never part of normal application
 * startup.
 */
@Service
public class FixtureRefreshService {
    private static final List<ReferenceEntityType> CORE_TYPES = List.of(
            ReferenceEntityType.SEASON,
            ReferenceEntityType.MANUFACTURER,
            ReferenceEntityType.CIRCUIT,
            ReferenceEntityType.TEAM,
            ReferenceEntityType.RIDER,
            ReferenceEntityType.EVENT,
            ReferenceEntityType.SESSION);

    private final ProviderAdapterRegistry providers;
    private final IdentityMappingStore identityStore;
    private final NamedParameterJdbcTemplate jdbc;
    private final ReferenceDataIngestionService referenceIngestion;
    private final RaceDataIngestionService raceIngestion;

    public FixtureRefreshService(ProviderAdapterRegistry providers,
                                 IdentityMappingStore identityStore,
                                 NamedParameterJdbcTemplate jdbc,
                                 ReferenceDataIngestionService referenceIngestion,
                                 RaceDataIngestionService raceIngestion) {
        this.providers = providers;
        this.identityStore = identityStore;
        this.jdbc = jdbc;
        this.referenceIngestion = referenceIngestion;
        this.raceIngestion = raceIngestion;
    }

    @Transactional
    public void refreshAndIngest(String providerCode) {
        if (!FixtureHistoricalDataProvider.CODE.equals(providerCode)) {
            throw new IllegalArgumentException("Fixture refresh only supports " + FixtureHistoricalDataProvider.CODE);
        }
        HistoricalDataProvider provider = providers.require(providerCode);
        UUID providerId = identityStore.providerId(providerCode);
        Map<ReferenceEntityType, List<String>> desiredExternalIds = desiredReferenceIds(provider);

        if (!raceDataMatches(providerId, provider)) {
            removeFixtureRaceData(providerId);
        }
        if (!relationshipsMatch(providerId, provider)) {
            removeFixtureRelationships(providerId);
        }
        removeObsoleteReferenceData(providerId, desiredExternalIds);

        referenceIngestion.ingest(providerCode);
        raceIngestion.ingest(providerCode);
    }

    private Map<ReferenceEntityType, List<String>> desiredReferenceIds(HistoricalDataProvider provider) {
        Map<ReferenceEntityType, List<String>> desired = new EnumMap<>(ReferenceEntityType.class);
        for (ReferenceEntityType type : CORE_TYPES) {
            desired.put(type, provider.findReferenceData(new ReferenceQuery(type, null)).stream()
                    .map(ReferenceData::externalId).toList());
        }
        return desired;
    }

    private void removeFixtureRaceData(UUID providerId) {
        deleteOwnedRows("race_positions", providerId, "RACE_POSITION");
        deleteOwnedRows("rider_laps", providerId, "RIDER_LAP");
        deleteOwnedRows("session_results", providerId, "SESSION_RESULT");
        deleteSourceRecords(providerId, "RACE_POSITION");
        deleteSourceRecords(providerId, "RIDER_LAP");
        deleteSourceRecords(providerId, "SESSION_RESULT");
    }

    private void removeFixtureRelationships(UUID providerId) {
        deleteOwnedRows("team_manufacturer_seasons", providerId, "TEAM_MANUFACTURER_SEASON");
        deleteOwnedRows("rider_season_entries", providerId, "RIDER_SEASON_ENTRY");
        deleteSourceRecords(providerId, "TEAM_MANUFACTURER_SEASON");
        deleteSourceRecords(providerId, "RIDER_SEASON_ENTRY");
    }

    private boolean relationshipsMatch(UUID providerId, HistoricalDataProvider provider) {
        Set<String> desiredAffiliations = provider.findReferenceRelationships().stream()
                .filter(record -> record.relationshipType() == ReferenceRelationshipType.TEAM_MANUFACTURER_SEASON)
                .map(ReferenceRelationshipData::externalId).collect(java.util.stream.Collectors.toSet());
        Set<String> desiredEntries = provider.findReferenceRelationships().stream()
                .filter(record -> record.relationshipType() == ReferenceRelationshipType.RIDER_SEASON_ENTRY)
                .map(ReferenceRelationshipData::externalId).collect(java.util.stream.Collectors.toSet());
        return sourceExternalIds(providerId, "TEAM_MANUFACTURER_SEASON").equals(desiredAffiliations)
                && sourceExternalIds(providerId, "RIDER_SEASON_ENTRY").equals(desiredEntries);
    }

    private boolean raceDataMatches(UUID providerId, HistoricalDataProvider provider) {
        List<SessionResultReference> results = provider.findRaceData(new RaceDataQuery(RaceDataType.SESSION_RESULT, null))
                .stream().map(SessionResultReference.class::cast).toList();
        List<LapReference> laps = provider.findRaceData(new RaceDataQuery(RaceDataType.RIDER_LAP, null))
                .stream().map(LapReference.class::cast).toList();
        List<RacePositionReference> positions = provider.findRaceData(new RaceDataQuery(RaceDataType.RACE_POSITION, null))
                .stream().map(RacePositionReference.class::cast).toList();
        return raceResultsMatch(providerId, results)
                && lapsMatch(providerId, laps)
                && positionsMatch(providerId, positions);
    }

    private boolean raceResultsMatch(UUID providerId, List<SessionResultReference> records) {
        if (!sourceCountMatches(providerId, "SESSION_RESULT", records.size())) return false;
        for (SessionResultReference record : records) {
            Boolean matches = jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM session_results sr
                        JOIN source_records source ON source.id = sr.primary_source_record_id
                        WHERE source.provider_id = :providerId AND source.entity_type = 'SESSION_RESULT'
                          AND source.provider_external_id = :externalId
                          AND sr.grid_position IS NOT DISTINCT FROM :gridPosition
                          AND sr.classification_position IS NOT DISTINCT FROM :classificationPosition
                          AND sr.classification_status = :classificationStatus
                          AND sr.laps_completed IS NOT DISTINCT FROM :lapsCompleted
                          AND sr.total_time_ms IS NOT DISTINCT FROM :totalTimeMs
                          AND sr.points IS NOT DISTINCT FROM :points)
                    """, raceParameters(providerId, record.externalId())
                    .addValue("gridPosition", record.gridPosition())
                    .addValue("classificationPosition", record.classificationPosition())
                    .addValue("classificationStatus", record.classificationStatus())
                    .addValue("lapsCompleted", record.lapsCompleted())
                    .addValue("totalTimeMs", record.totalTimeMs())
                    .addValue("points", record.points()), Boolean.class);
            if (!Boolean.TRUE.equals(matches)) return false;
        }
        return true;
    }

    private boolean lapsMatch(UUID providerId, List<LapReference> records) {
        if (!sourceCountMatches(providerId, "RIDER_LAP", records.size())) return false;
        for (LapReference record : records) {
            Boolean matches = jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM rider_laps lap
                        JOIN source_records source ON source.id = lap.primary_source_record_id
                        WHERE source.provider_id = :providerId AND source.entity_type = 'RIDER_LAP'
                          AND source.provider_external_id = :externalId
                          AND lap.lap_number = :lapNumber
                          AND lap.lap_time_ms IS NOT DISTINCT FROM :lapTimeMs
                          AND lap.is_valid IS NOT DISTINCT FROM :isValid
                          AND lap.observed_at IS NOT DISTINCT FROM :observedAt)
                    """, raceParameters(providerId, record.externalId())
                    .addValue("lapNumber", record.lapNumber())
                    .addValue("lapTimeMs", record.lapTimeMs())
                    .addValue("isValid", record.valid())
                    .addValue("observedAt", timestamp(record.observedAt())), Boolean.class);
            if (!Boolean.TRUE.equals(matches)) return false;
        }
        return true;
    }

    private boolean positionsMatch(UUID providerId, List<RacePositionReference> records) {
        if (!sourceCountMatches(providerId, "RACE_POSITION", records.size())) return false;
        for (RacePositionReference record : records) {
            Boolean matches = jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM race_positions position
                        JOIN source_records source ON source.id = position.primary_source_record_id
                        WHERE source.provider_id = :providerId AND source.entity_type = 'RACE_POSITION'
                          AND source.provider_external_id = :externalId
                          AND position.lap_number = :lapNumber
                          AND position.position IS NOT DISTINCT FROM :position
                          AND position.observed_at IS NOT DISTINCT FROM :observedAt)
                    """, raceParameters(providerId, record.externalId())
                    .addValue("lapNumber", record.lapNumber())
                    .addValue("position", record.position())
                    .addValue("observedAt", timestamp(record.observedAt())), Boolean.class);
            if (!Boolean.TRUE.equals(matches)) return false;
        }
        return true;
    }

    private boolean sourceCountMatches(UUID providerId, String entityType, int expected) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM source_records
                WHERE provider_id = :providerId AND entity_type = :entityType
                """, new MapSqlParameterSource().addValue("providerId", providerId)
                .addValue("entityType", entityType), Long.class);
        return count != null && count == expected;
    }

    private Set<String> sourceExternalIds(UUID providerId, String entityType) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT provider_external_id FROM source_records
                WHERE provider_id = :providerId AND entity_type = :entityType
                """, new MapSqlParameterSource().addValue("providerId", providerId)
                .addValue("entityType", entityType), String.class));
    }

    private MapSqlParameterSource raceParameters(UUID providerId, String externalId) {
        return new MapSqlParameterSource().addValue("providerId", providerId).addValue("externalId", externalId);
    }

    private Timestamp timestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private void removeObsoleteReferenceData(UUID providerId,
                                             Map<ReferenceEntityType, List<String>> desiredExternalIds) {
        // Children must be removed before their event/season/circuit/team dependencies.
        removeObsoleteCoreType(providerId, ReferenceEntityType.SESSION, desiredExternalIds.get(ReferenceEntityType.SESSION));
        removeObsoleteCoreType(providerId, ReferenceEntityType.EVENT, desiredExternalIds.get(ReferenceEntityType.EVENT));
        removeObsoleteCoreType(providerId, ReferenceEntityType.RIDER, desiredExternalIds.get(ReferenceEntityType.RIDER));
        removeObsoleteCoreType(providerId, ReferenceEntityType.TEAM, desiredExternalIds.get(ReferenceEntityType.TEAM));
        removeObsoleteCoreType(providerId, ReferenceEntityType.MANUFACTURER, desiredExternalIds.get(ReferenceEntityType.MANUFACTURER));
        removeObsoleteCoreType(providerId, ReferenceEntityType.CIRCUIT, desiredExternalIds.get(ReferenceEntityType.CIRCUIT));
        removeObsoleteCoreType(providerId, ReferenceEntityType.SEASON, desiredExternalIds.get(ReferenceEntityType.SEASON));
    }

    private void removeObsoleteCoreType(UUID providerId, ReferenceEntityType type, List<String> desiredIds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("providerId", providerId)
                .addValue("entityType", type.name())
                .addValue("desiredIds", desiredIds);
        String table = type.tableName();

        jdbc.update("DELETE FROM provider_identity_mappings "
                        + "WHERE provider_id = :providerId AND entity_type = :entityType "
                        + "AND provider_external_id NOT IN (:desiredIds)", params);
        jdbc.update("DELETE FROM " + table + " WHERE primary_source_record_id IN ("
                        + "SELECT id FROM source_records WHERE provider_id = :providerId "
                        + "AND entity_type = :entityType AND provider_external_id NOT IN (:desiredIds))", params);
        deleteSourceRecordsExcept(providerId, type.name(), desiredIds);
    }

    private void deleteOwnedRows(String table, UUID providerId, String entityType) {
        jdbc.update("DELETE FROM " + table + " WHERE primary_source_record_id IN ("
                        + "SELECT id FROM source_records WHERE provider_id = :providerId AND entity_type = :entityType)",
                new MapSqlParameterSource().addValue("providerId", providerId).addValue("entityType", entityType));
    }

    private void deleteSourceRecords(UUID providerId, String entityType) {
        jdbc.update("DELETE FROM source_records WHERE provider_id = :providerId AND entity_type = :entityType",
                new MapSqlParameterSource().addValue("providerId", providerId).addValue("entityType", entityType));
    }

    private void deleteSourceRecordsExcept(UUID providerId, String entityType, List<String> desiredIds) {
        jdbc.update("DELETE FROM source_records WHERE provider_id = :providerId AND entity_type = :entityType "
                        + "AND provider_external_id NOT IN (:desiredIds)",
                new MapSqlParameterSource().addValue("providerId", providerId).addValue("entityType", entityType)
                        .addValue("desiredIds", desiredIds));
    }
}
