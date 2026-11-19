package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class FixtureRefreshComposeIntegrationTest {
    @Autowired
    private FixtureRefreshService refresh;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void repairsCorruptFixtureRowsAndIsIdempotent() {
        UUID otherProvider = jdbc.queryForObject("""
                INSERT INTO providers (code, name, kind, enabled)
                VALUES (:code, 'Unrelated Test Provider', 'FIXTURE', true)
                RETURNING id
                """, new MapSqlParameterSource().addValue("code", "unrelated-" + UUID.randomUUID()), UUID.class);
        String otherExternalId = "unrelated-rider-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO source_records (provider_id, entity_type, provider_external_id,
                    source_version, first_seen_at, last_seen_at)
                VALUES (:providerId, 'RIDER', :externalId, 'other-v1', :observedAt, :observedAt)
                """, new MapSqlParameterSource().addValue("providerId", otherProvider)
                .addValue("externalId", otherExternalId).addValue("observedAt", java.sql.Timestamp.from(Instant.now())));

        refresh.refreshAndIngest(FixtureHistoricalDataProvider.CODE);
        corruptFixtureRows();
        refresh.refreshAndIngest(FixtureHistoricalDataProvider.CODE);
        assertExpandedCounts();
        assertThat(sourceRecordExists(otherProvider, otherExternalId)).isTrue();

        refresh.refreshAndIngest(FixtureHistoricalDataProvider.CODE);
        assertExpandedCounts();
        assertThat(sourceRecordExists(otherProvider, otherExternalId)).isTrue();
        assertThat(fixtureSourceCount("RACE_POSITION")).isEqualTo(440);
        assertThat(fixtureSourceCount("RIDER_LAP")).isEqualTo(400);
        assertThat(fixtureSourceCount("SESSION_RESULT")).isEqualTo(120);

        long baselineAfterExternalChange = count("race_positions") - 1;
        jdbc.update("DELETE FROM race_positions WHERE id = (SELECT id FROM race_positions LIMIT 1)",
                new MapSqlParameterSource());
        jdbc.update("""
                CREATE OR REPLACE FUNCTION fixture_refresh_test_failure() RETURNS trigger
                LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture refresh test failure'; END; $$
                """, new MapSqlParameterSource());
        jdbc.update("""
                CREATE TRIGGER fixture_refresh_test_failure_trigger
                BEFORE INSERT ON race_positions
                FOR EACH ROW EXECUTE FUNCTION fixture_refresh_test_failure()
                """, new MapSqlParameterSource());
        try {
            assertThatThrownBy(() -> refresh.refreshAndIngest(FixtureHistoricalDataProvider.CODE))
                    .isInstanceOf(RuntimeException.class);
            assertThat(count("race_positions")).isEqualTo(baselineAfterExternalChange);
            assertThat(jdbc.queryForMap("""
                    SELECT ia.status, ia.records_written, ia.error_summary
                    FROM ingestion_attempts ia
                    JOIN ingestion_jobs ij ON ij.id = ia.job_id
                    JOIN providers p ON p.id = ij.provider_id
                    WHERE p.code = :provider AND ij.job_type = 'HISTORICAL'
                    ORDER BY ia.started_at DESC LIMIT 1
                    """, Map.of("provider", FixtureHistoricalDataProvider.CODE)))
                    .containsEntry("status", "FAILED")
                    .containsEntry("records_written", 0)
                    .doesNotContainEntry("error_summary", null);
        } finally {
            jdbc.update("DROP TRIGGER IF EXISTS fixture_refresh_test_failure_trigger ON race_positions",
                    new MapSqlParameterSource());
            jdbc.update("DROP FUNCTION IF EXISTS fixture_refresh_test_failure()", new MapSqlParameterSource());
        }
    }

    private void corruptFixtureRows() {
        jdbc.update("""
                UPDATE session_results sr SET laps_completed = 3, total_time_ms = 123456
                FROM source_records source
                WHERE source.id = sr.primary_source_record_id
                  AND source.provider_id = (SELECT id FROM providers WHERE code = :provider)
                  AND source.provider_external_id = 'fixture-result-rider-001'
                """, Map.of("provider", FixtureHistoricalDataProvider.CODE));
        jdbc.update("""
                DELETE FROM rider_laps lap
                USING source_records source
                WHERE source.id = lap.primary_source_record_id
                  AND source.provider_id = (SELECT id FROM providers WHERE code = :provider)
                  AND source.provider_external_id = 'fixture-lap-fixture-session-race-2025-fixture-rider-001-1'
                """, Map.of("provider", FixtureHistoricalDataProvider.CODE));
        jdbc.update("""
                UPDATE rider_laps lap SET lap_time_ms = 99999
                FROM source_records source
                WHERE source.id = lap.primary_source_record_id
                  AND source.provider_id = (SELECT id FROM providers WHERE code = :provider)
                  AND source.provider_external_id = 'fixture-lap-fixture-session-race-2025-fixture-rider-001-2'
                """, Map.of("provider", FixtureHistoricalDataProvider.CODE));
    }

    private void assertExpandedCounts() {
        assertThat(count("seasons")).isEqualTo(2);
        assertThat(count("manufacturers")).isEqualTo(2);
        assertThat(count("teams")).isEqualTo(4);
        assertThat(count("riders")).isEqualTo(10);
        assertThat(count("circuits")).isEqualTo(2);
        assertThat(count("events")).isEqualTo(4);
        assertThat(count("sessions")).isEqualTo(12);
        assertThat(count("session_results")).isEqualTo(120);
        assertThat(count("rider_laps")).isEqualTo(400);
        assertThat(count("race_positions")).isEqualTo(440);
        assertThat(count("team_manufacturer_seasons")).isEqualTo(8);
        assertThat(count("rider_season_entries")).isEqualTo(20);
        assertThat(jdbc.queryForObject("""
                SELECT laps_completed FROM session_results sr
                JOIN source_records source ON source.id = sr.primary_source_record_id
                WHERE source.provider_external_id = 'fixture-result-rider-001'
                """, Map.of(), Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("""
                SELECT lap_time_ms FROM rider_laps lap
                JOIN source_records source ON source.id = lap.primary_source_record_id
                WHERE source.provider_external_id = 'fixture-lap-fixture-session-race-2025-fixture-rider-001-2'
                """, Map.of(), Integer.class)).isNotEqualTo(99999);
    }

    private long count(String table) {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long fixtureSourceCount(String entityType) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM source_records source
                JOIN providers provider ON provider.id = source.provider_id
                WHERE provider.code = :provider AND source.entity_type = :entityType
                """, new MapSqlParameterSource().addValue("provider", FixtureHistoricalDataProvider.CODE)
                .addValue("entityType", entityType), Long.class);
    }

    private boolean sourceRecordExists(UUID providerId, String externalId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM source_records
                WHERE provider_id = :providerId AND provider_external_id = :externalId)
                """, new MapSqlParameterSource().addValue("providerId", providerId)
                .addValue("externalId", externalId), Boolean.class));
    }
}
