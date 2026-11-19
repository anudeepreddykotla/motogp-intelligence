package com.motogp.intelligence.news;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class NewsSourceStore {
    private final NamedParameterJdbcTemplate jdbc;

    public NewsSourceStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID upsert(NewsSourceDefinition source, UUID providerId) {
        return jdbc.queryForObject("""
                INSERT INTO news_sources (provider_id, code, name, source_type, feed_url, enabled, timeout_ms)
                VALUES (:providerId, :code, :name, :sourceType, :feedUrl, :enabled, :timeoutMs)
                ON CONFLICT (code) DO UPDATE SET
                    provider_id = EXCLUDED.provider_id, name = EXCLUDED.name,
                    source_type = EXCLUDED.source_type, feed_url = EXCLUDED.feed_url,
                    enabled = EXCLUDED.enabled, timeout_ms = EXCLUDED.timeout_ms, updated_at = now()
                RETURNING id
                """, params(source, providerId), UUID.class);
    }

    public void markAttempt(UUID sourceId, Instant attemptedAt) {
        jdbc.update("UPDATE news_sources SET last_attempted_at = :at, updated_at = now() WHERE id = :id",
                new MapSqlParameterSource().addValue("id", sourceId).addValue("at", Timestamp.from(attemptedAt)));
    }

    public void markSuccess(UUID sourceId, Instant completedAt) {
        jdbc.update("""
                UPDATE news_sources SET last_attempted_at = :at, last_successful_fetch_at = :at,
                    last_failed_at = NULL, last_error = NULL, updated_at = now() WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", sourceId).addValue("at", Timestamp.from(completedAt)));
    }

    public void markFailure(UUID sourceId, Instant failedAt, String error) {
        jdbc.update("""
                UPDATE news_sources SET last_attempted_at = :at, last_failed_at = :at,
                    last_error = :error, updated_at = now() WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", sourceId).addValue("at", Timestamp.from(failedAt))
                .addValue("error", error));
    }

    private MapSqlParameterSource params(NewsSourceDefinition source, UUID providerId) {
        return new MapSqlParameterSource().addValue("providerId", providerId).addValue("code", source.code())
                .addValue("name", source.name()).addValue("sourceType", source.sourceType().toUpperCase())
                .addValue("feedUrl", source.feedUrl()).addValue("enabled", source.enabled())
                .addValue("timeoutMs", source.timeoutMs());
    }
}
