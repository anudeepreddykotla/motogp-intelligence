package com.motogp.intelligence.ingestion;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class IngestionRunStore {
    private final NamedParameterJdbcTemplate jdbc;

    public IngestionRunStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID ensureReferenceJob(String providerCode) {
        return ensureJob(providerCode, "REFERENCE");
    }

    public UUID ensureHistoricalJob(String providerCode) {
        return ensureJob(providerCode, "HISTORICAL");
    }

    private UUID ensureJob(String providerCode, String jobType) {
        return jdbc.queryForObject("""
                        INSERT INTO ingestion_jobs (provider_id, job_type)
                        SELECT id, :jobType FROM providers WHERE code = :providerCode
                        ON CONFLICT (provider_id, job_type) DO UPDATE SET updated_at = now()
                        RETURNING id
                        """, new MapSqlParameterSource().addValue("providerCode", providerCode)
                        .addValue("jobType", jobType), UUID.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID startAttempt(UUID jobId, Instant startedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO ingestion_attempts (job_id, started_at, status)
                        VALUES (:jobId, :startedAt, 'RUNNING') RETURNING id
                        """, new MapSqlParameterSource().addValue("jobId", jobId)
                        .addValue("startedAt", Timestamp.from(startedAt)), UUID.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishAttempt(UUID attemptId, String status, int recordsSeen,
                              int recordsWritten, Instant finishedAt) {
        jdbc.update("""
                        UPDATE ingestion_attempts
                        SET finished_at = :finishedAt, status = :status,
                            records_seen = :recordsSeen, records_written = :recordsWritten
                        WHERE id = :attemptId
                        """, new MapSqlParameterSource().addValue("attemptId", attemptId)
                        .addValue("finishedAt", Timestamp.from(finishedAt)).addValue("status", status)
                        .addValue("recordsSeen", recordsSeen).addValue("recordsWritten", recordsWritten));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishFailedAttempt(UUID attemptId, int recordsSeen, String errorSummary, Instant finishedAt) {
        jdbc.update("""
                        UPDATE ingestion_attempts
                        SET finished_at = :finishedAt, status = 'FAILED',
                            records_seen = :recordsSeen, records_written = 0,
                            error_summary = :errorSummary
                        WHERE id = :attemptId
                        """, new MapSqlParameterSource().addValue("attemptId", attemptId)
                        .addValue("finishedAt", Timestamp.from(finishedAt)).addValue("recordsSeen", recordsSeen)
                        .addValue("errorSummary", errorSummary));
    }
}
