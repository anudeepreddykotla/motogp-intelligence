package com.motogp.intelligence.ingestion;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RaceDataStore {
    private final NamedParameterJdbcTemplate jdbc;

    public RaceDataStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SessionResultRow> findSessionResult(UUID sessionId, UUID riderId) {
        return jdbc.query("""
                        SELECT id, session_id, rider_id, team_id, manufacturer_id, grid_position,
                               classification_position, classification_status, laps_completed, total_time_ms, points
                        FROM session_results WHERE session_id = :sessionId AND rider_id = :riderId
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId),
                rows -> rows.next() ? Optional.of(new SessionResultRow(rows.getObject("id", UUID.class),
                        rows.getObject("session_id", UUID.class), rows.getObject("rider_id", UUID.class),
                        rows.getObject("team_id", UUID.class), rows.getObject("manufacturer_id", UUID.class),
                        number(rows.getObject("grid_position")), number(rows.getObject("classification_position")),
                        rows.getString("classification_status"), number(rows.getObject("laps_completed")),
                        (Integer) rows.getObject("total_time_ms"), rows.getBigDecimal("points"))) : Optional.empty());
    }

    public UUID insertSessionResult(SessionResultReference data, UUID sessionId, UUID riderId,
                                    UUID teamId, UUID manufacturerId) {
        return jdbc.queryForObject("""
                        INSERT INTO session_results (
                            session_id, rider_id, team_id, manufacturer_id, grid_position,
                            classification_position, classification_status, laps_completed, total_time_ms, points)
                        VALUES (:sessionId, :riderId, :teamId, :manufacturerId, :gridPosition,
                                :classificationPosition, :classificationStatus, :lapsCompleted, :totalTimeMs, :points)
                        RETURNING id
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId)
                        .addValue("teamId", teamId).addValue("manufacturerId", manufacturerId)
                        .addValue("gridPosition", data.gridPosition()).addValue("classificationPosition", data.classificationPosition())
                        .addValue("classificationStatus", data.classificationStatus()).addValue("lapsCompleted", data.lapsCompleted())
                        .addValue("totalTimeMs", data.totalTimeMs()).addValue("points", data.points()), UUID.class);
    }

    public Optional<LapRow> findLap(UUID sessionResultId, short lapNumber) {
        return jdbc.query("SELECT id, session_result_id, lap_number, lap_time_ms, is_valid, observed_at FROM rider_laps "
                        + "WHERE session_result_id = :resultId AND lap_number = :lapNumber",
                params().addValue("resultId", sessionResultId).addValue("lapNumber", lapNumber),
                rows -> rows.next() ? Optional.of(new LapRow(rows.getObject("id", UUID.class),
                        rows.getObject("session_result_id", UUID.class), number(rows.getObject("lap_number")),
                        (Integer) rows.getObject("lap_time_ms"), (Boolean) rows.getObject("is_valid"),
                        instant(rows.getTimestamp("observed_at")))) : Optional.empty());
    }

    public UUID insertLap(LapReference data, UUID sessionResultId) {
        return jdbc.queryForObject("""
                        INSERT INTO rider_laps (session_result_id, lap_number, lap_time_ms, is_valid, observed_at)
                        VALUES (:resultId, :lapNumber, :lapTimeMs, :isValid, :observedAt) RETURNING id
                        """, params().addValue("resultId", sessionResultId).addValue("lapNumber", data.lapNumber())
                        .addValue("lapTimeMs", data.lapTimeMs()).addValue("isValid", data.valid())
                        .addValue("observedAt", timestamp(data.observedAt())), UUID.class);
    }

    public Optional<PositionRow> findPosition(UUID sessionResultId, short lapNumber) {
        return jdbc.query("SELECT id, session_result_id, lap_number, position, observed_at FROM race_positions "
                        + "WHERE session_result_id = :resultId AND lap_number = :lapNumber",
                params().addValue("resultId", sessionResultId).addValue("lapNumber", lapNumber),
                rows -> rows.next() ? Optional.of(new PositionRow(rows.getObject("id", UUID.class),
                        rows.getObject("session_result_id", UUID.class), number(rows.getObject("lap_number")),
                        number(rows.getObject("position")), instant(rows.getTimestamp("observed_at")))) : Optional.empty());
    }

    public UUID insertPosition(RacePositionReference data, UUID sessionResultId) {
        return jdbc.queryForObject("""
                        INSERT INTO race_positions (session_result_id, lap_number, position, observed_at)
                        VALUES (:resultId, :lapNumber, :position, :observedAt) RETURNING id
                        """, params().addValue("resultId", sessionResultId).addValue("lapNumber", data.lapNumber())
                        .addValue("position", data.position()).addValue("observedAt", timestamp(data.observedAt())), UUID.class);
    }

    public void attachPrimarySourceRecord(String tableName, UUID rowId, UUID sourceRecordId) {
        if (!java.util.Set.of("session_results", "rider_laps", "race_positions").contains(tableName)) {
            throw new IllegalArgumentException("Unsupported race table: " + tableName);
        }
        jdbc.update("UPDATE " + tableName
                        + " SET primary_source_record_id = COALESCE(primary_source_record_id, :sourceRecordId), updated_at = now()"
                        + " WHERE id = :rowId", params().addValue("sourceRecordId", sourceRecordId)
                        .addValue("rowId", rowId));
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private Short number(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }

    public record SessionResultRow(UUID id, UUID sessionId, UUID riderId, UUID teamId, UUID manufacturerId,
                                   Short gridPosition, Short classificationPosition, String classificationStatus,
                                   Short lapsCompleted, Integer totalTimeMs, BigDecimal points) {
    }

    public record LapRow(UUID id, UUID sessionResultId, Short lapNumber, Integer lapTimeMs,
                         Boolean valid, Instant observedAt) {
    }

    public record PositionRow(UUID id, UUID sessionResultId, Short lapNumber, Short position,
                              Instant observedAt) {
    }
}
