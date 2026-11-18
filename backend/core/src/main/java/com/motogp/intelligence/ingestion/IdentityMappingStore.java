package com.motogp.intelligence.ingestion;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class IdentityMappingStore {
    private final NamedParameterJdbcTemplate jdbc;

    public IdentityMappingStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID providerId(String providerCode) {
        return jdbc.queryForObject("SELECT id FROM providers WHERE code = :code",
                new MapSqlParameterSource("code", providerCode), UUID.class);
    }

    public void requireInternalEntity(ReferenceEntityType entityType, UUID internalId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM " + entityType.tableName() + " WHERE id = :id)";
        Boolean exists = jdbc.queryForObject(sql, new MapSqlParameterSource("id", internalId), Boolean.class);
        if (!Boolean.TRUE.equals(exists)) {
            throw new InvalidInternalEntityException(entityType, internalId);
        }
    }

    public ExistingMapping findMapping(UUID providerId, ReferenceEntityType entityType, String externalId) {
        return jdbc.query("""
                        SELECT id, internal_id
                        FROM provider_identity_mappings
                        WHERE provider_id = :providerId AND entity_type = :entityType
                          AND provider_external_id = :externalId
                        """,
                new MapSqlParameterSource()
                        .addValue("providerId", providerId)
                        .addValue("entityType", entityType.name())
                        .addValue("externalId", externalId),
                rows -> rows.next() ? new ExistingMapping(
                        rows.getObject("id", UUID.class), rows.getObject("internal_id", UUID.class)) : null);
    }

    public UUID upsertSourceRecord(UUID providerId, ReferenceEntityType entityType,
                                   String externalId, String sourceVersion, Instant observedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO source_records (
                            provider_id, entity_type, provider_external_id, source_version, first_seen_at, last_seen_at)
                        VALUES (:providerId, :entityType, :externalId, :sourceVersion, :observedAt, :observedAt)
                        ON CONFLICT (provider_id, entity_type, provider_external_id)
                        DO UPDATE SET last_seen_at = GREATEST(source_records.last_seen_at, EXCLUDED.last_seen_at),
                                      source_version = COALESCE(EXCLUDED.source_version, source_records.source_version)
                        RETURNING id
                        """,
                new MapSqlParameterSource()
                        .addValue("providerId", providerId)
                        .addValue("entityType", entityType.name())
                        .addValue("externalId", externalId)
                        .addValue("sourceVersion", sourceVersion)
                        .addValue("observedAt", observedAt), UUID.class);
    }

    public UUID insertMapping(UUID providerId, ReferenceEntityType entityType, String externalId,
                              UUID internalId, Instant observedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO provider_identity_mappings (
                            provider_id, entity_type, provider_external_id, internal_id, first_seen_at, last_seen_at)
                        VALUES (:providerId, :entityType, :externalId, :internalId, :observedAt, :observedAt)
                        RETURNING id
                        """,
                new MapSqlParameterSource()
                        .addValue("providerId", providerId)
                        .addValue("entityType", entityType.name())
                        .addValue("externalId", externalId)
                        .addValue("internalId", internalId)
                        .addValue("observedAt", observedAt), UUID.class);
    }

    public record ExistingMapping(UUID mappingId, UUID internalId) {
    }
}
