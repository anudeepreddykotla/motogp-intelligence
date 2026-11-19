package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class ReferenceDataComposeIntegrationTest {
    @Autowired
    private ReferenceDataIngestionService ingestion;

    @Autowired
    private RaceDataIngestionService raceIngestion;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private IdentityMappingStore identityMappingStore;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void ingestsFixtureIntoComposePostgresAndIsIdempotent() {
        ReferenceIngestionResult first = ingestion.ingest(FixtureHistoricalDataProvider.CODE);
        ReferenceIngestionResult second = ingestion.ingest(FixtureHistoricalDataProvider.CODE);

        assertThat(first.recordsSeen()).isEqualTo(64);
        assertThat(second.recordsWritten()).isZero();
        RaceDataIngestionResult raceFirst = raceIngestion.ingest(FixtureHistoricalDataProvider.CODE);
        RaceDataIngestionResult raceSecond = raceIngestion.ingest(FixtureHistoricalDataProvider.CODE);
        assertThat(raceFirst.recordsSeen()).isEqualTo(960);
        assertThat(raceSecond.recordsWritten()).isZero();
        assertThat(count("seasons")).isEqualTo(2);
        assertThat(count("manufacturers")).isEqualTo(2);
        assertThat(count("teams")).isEqualTo(4);
        assertThat(count("riders")).isEqualTo(10);
        assertThat(count("circuits")).isEqualTo(2);
        assertThat(count("events")).isEqualTo(4);
        assertThat(count("team_manufacturer_seasons")).isEqualTo(8);
        assertThat(count("rider_season_entries")).isEqualTo(20);
        assertThat(count("sessions")).isEqualTo(12);
        assertThat(count("session_results")).isEqualTo(120);
        assertThat(count("rider_laps")).isEqualTo(400);
        assertThat(count("race_positions")).isEqualTo(440);
        assertThat(jdbc.getJdbcTemplate().queryForObject("""
                SELECT count(*) FROM session_results sr LEFT JOIN sessions s ON s.id = sr.session_id
                LEFT JOIN riders r ON r.id = sr.rider_id
                WHERE s.id IS NULL OR r.id IS NULL
                """, Long.class)).isZero();
        assertThat(jdbc.getJdbcTemplate().queryForObject("""
                SELECT count(*) FROM rider_laps l LEFT JOIN session_results sr ON sr.id = l.session_result_id
                WHERE sr.id IS NULL
                """, Long.class)).isZero();
        assertThat(jdbc.getJdbcTemplate().queryForObject("""
                SELECT count(*) FROM race_positions p LEFT JOIN session_results sr ON sr.id = p.session_result_id
                WHERE sr.id IS NULL
                """, Long.class)).isZero();
    }

    @Test
    void failedRacePersistenceRollsBackRowsButCommitsFailedAttempt() {
        ingestion.ingest(FixtureHistoricalDataProvider.CODE);
        raceIngestion.ingest(FixtureHistoricalDataProvider.CODE);

        long baselineLapCount = count("rider_laps");
        Instant sourceLastSeenBefore = jdbc.queryForObject("""
                        SELECT last_seen_at FROM source_records
                        WHERE provider_external_id = 'fixture-result-rider-001'
                        """, java.util.Map.of(), Instant.class);

        jdbc.update("""
                DELETE FROM rider_laps
                WHERE session_result_id = (
                    SELECT sr.id FROM session_results sr
                    JOIN riders r ON r.id = sr.rider_id
                    JOIN sessions s ON s.id = sr.session_id
                    JOIN events e ON e.id = s.event_id
                    WHERE r.display_name = 'Fixture Rider One' AND s.session_type = 'RACE' AND e.name = 'Italian Grand Prix'
                ) AND lap_number = 1
                """, java.util.Map.of());
        jdbc.update("""
                UPDATE rider_laps
                SET lap_time_ms = 99999
                WHERE session_result_id = (
                    SELECT sr.id FROM session_results sr
                    JOIN riders r ON r.id = sr.rider_id
                    JOIN sessions s ON s.id = sr.session_id
                    JOIN events e ON e.id = s.event_id
                    WHERE r.display_name = 'Fixture Rider One' AND s.session_type = 'RACE' AND e.name = 'Italian Grand Prix'
                ) AND lap_number = 2
                """, java.util.Map.of());

        assertThatThrownBy(() -> raceIngestion.ingest(FixtureHistoricalDataProvider.CODE))
                .isInstanceOf(RaceDataConflictException.class);

        assertThat(count("rider_laps")).isEqualTo(baselineLapCount - 1);
        assertThat(jdbc.queryForObject("""
                        SELECT lap_time_ms FROM rider_laps
                        WHERE session_result_id = (
                            SELECT sr.id FROM session_results sr
                            JOIN riders r ON r.id = sr.rider_id
                            JOIN sessions s ON s.id = sr.session_id
                            JOIN events e ON e.id = s.event_id
                            WHERE r.display_name = 'Fixture Rider One' AND s.session_type = 'RACE' AND e.name = 'Italian Grand Prix'
                        ) AND lap_number = 2
                        """, java.util.Map.of(), Integer.class)).isEqualTo(99999);
        assertThat(jdbc.queryForObject("""
                        SELECT last_seen_at FROM source_records
                        WHERE provider_external_id = 'fixture-result-rider-001'
                        """, java.util.Map.of(), Instant.class)).isEqualTo(sourceLastSeenBefore);

        java.util.Map<String, Object> failedAttempt = jdbc.queryForMap("""
                SELECT ia.status, ia.records_written, ia.error_summary
                FROM ingestion_attempts ia
                JOIN ingestion_jobs ij ON ij.id = ia.job_id
                JOIN providers p ON p.id = ij.provider_id
                WHERE p.code = :provider AND ij.job_type = 'HISTORICAL'
                ORDER BY ia.started_at DESC LIMIT 1
                """, java.util.Map.of("provider", FixtureHistoricalDataProvider.CODE));
        assertThat(failedAttempt.get("status")).isEqualTo("FAILED");
        assertThat(((Number) failedAttempt.get("records_written")).intValue()).isZero();
        assertThat(failedAttempt.get("error_summary")).isNotNull();
    }

    @Test
    void sourceVersionIsImmutableAndSameVersionIsIdempotent() {
        String externalId = "source-version-test-" + UUID.randomUUID();
        UUID providerId = identityMappingStore.providerId(FixtureHistoricalDataProvider.CODE);
        UUID first = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, "fixture-v1", Instant.now());
        UUID same = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, "fixture-v1", Instant.now());

        assertThat(same).isEqualTo(first);
        assertThatThrownBy(() -> identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, "fixture-v2", Instant.now()))
                .isInstanceOf(SourceRecordConflictException.class)
                .hasMessageContaining("fixture-v1")
                .hasMessageContaining("fixture-v2");
        assertThat(jdbc.queryForObject("SELECT source_version FROM source_records WHERE id = :id",
                java.util.Map.of("id", first), String.class)).isEqualTo("fixture-v1");
    }

    @Test
    void preservesExistingSourceVersionWhenIncomingVersionIsNull() {
        String externalId = uniqueExternalId();
        UUID providerId = providerId();
        UUID first = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, "fixture-v1", Instant.now());
        UUID same = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, null, Instant.now());

        assertThat(same).isEqualTo(first);
        assertThat(sourceVersion(first)).isEqualTo("fixture-v1");
    }

    @Test
    void preservesExistingNullSourceVersionWhenIncomingVersionIsNonNull() {
        String externalId = uniqueExternalId();
        UUID providerId = providerId();
        UUID first = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, null, Instant.now());
        UUID same = identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                externalId, "fixture-v1", Instant.now());

        assertThat(same).isEqualTo(first);
        assertThat(sourceVersion(first)).isNull();
    }

    @Test
    void concurrentSameVersionInsertionReturnsOneId() throws Exception {
        String externalId = uniqueExternalId();
        UUID providerId = providerId();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<UUID>> futures = List.of(
                    executor.submit(concurrentUpsert(barrier, providerId, externalId, "fixture-v1")),
                    executor.submit(concurrentUpsert(barrier, providerId, externalId, "fixture-v1")));

            UUID first = futures.get(0).get(10, TimeUnit.SECONDS);
            UUID second = futures.get(1).get(10, TimeUnit.SECONDS);
            assertThat(second).isEqualTo(first);
            assertThat(countSourceRecords(providerId, externalId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentConflictingVersionsAcceptExactlyOneVersion() throws Exception {
        String externalId = uniqueExternalId();
        UUID providerId = providerId();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<UUID>> futures = List.of(
                    executor.submit(concurrentUpsert(barrier, providerId, externalId, "fixture-v1")),
                    executor.submit(concurrentUpsert(barrier, providerId, externalId, "fixture-v2")));

            UUID acceptedId = null;
            int conflicts = 0;
            for (Future<UUID> future : futures) {
                try {
                    UUID id = future.get(10, TimeUnit.SECONDS);
                    if (acceptedId == null) acceptedId = id;
                    assertThat(id).isEqualTo(acceptedId);
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(SourceRecordConflictException.class);
                    conflicts++;
                }
            }
            assertThat(acceptedId).isNotNull();
            assertThat(conflicts).isEqualTo(1);
            assertThat(countSourceRecords(providerId, externalId)).isEqualTo(1);
            assertThat(sourceVersion(acceptedId)).isIn("fixture-v1", "fixture-v2");
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<UUID> concurrentUpsert(CyclicBarrier barrier, UUID providerId,
                                             String externalId, String sourceVersion) {
        return () -> {
            barrier.await();
            return new TransactionTemplate(transactionManager).execute(status ->
                    identityMappingStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER,
                            externalId, sourceVersion, Instant.now()));
        };
    }

    private UUID providerId() {
        return identityMappingStore.providerId(FixtureHistoricalDataProvider.CODE);
    }

    private String uniqueExternalId() {
        return "source-version-concurrency-" + UUID.randomUUID();
    }

    private long countSourceRecords(UUID providerId, String externalId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM source_records
                WHERE provider_id = :providerId AND entity_type = 'RIDER'
                  AND provider_external_id = :externalId
                """, java.util.Map.of("providerId", providerId, "externalId", externalId), Long.class);
    }

    private String sourceVersion(UUID sourceRecordId) {
        return jdbc.queryForObject("SELECT source_version FROM source_records WHERE id = :id",
                java.util.Map.of("id", sourceRecordId), String.class);
    }

    private long count(String table) {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
