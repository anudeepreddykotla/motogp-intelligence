package com.motogp.intelligence.race;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class HistoricalRaceReadStore {
    private final NamedParameterJdbcTemplate jdbc;

    public HistoricalRaceReadStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SessionView> sessions(UUID eventId) {
        return jdbc.query("""
                        SELECT id, event_id, session_type, scheduled_start_at, started_at, ended_at, data_mode
                        FROM sessions WHERE event_id = :eventId ORDER BY scheduled_start_at NULLS LAST, session_type
                        """, params().addValue("eventId", eventId), (rs, row) -> new SessionView(
                        rs.getObject("id", UUID.class), rs.getObject("event_id", UUID.class),
                        rs.getString("session_type"), instant(rs.getTimestamp("scheduled_start_at")),
                        instant(rs.getTimestamp("started_at")), instant(rs.getTimestamp("ended_at")),
                        rs.getString("data_mode")));
    }

    public boolean eventExists(UUID eventId) {
        return exists("events", eventId);
    }

    public boolean sessionExists(UUID sessionId) {
        return exists("sessions", sessionId);
    }

    public boolean riderExists(UUID riderId) {
        return exists("riders", riderId);
    }

    private boolean exists(String table, UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id=:id)",
                params().addValue("id", id), Boolean.class));
    }

    public List<SessionResultView> results(UUID sessionId) {
        return jdbc.query("""
                        SELECT sr.id, sr.session_id, sr.rider_id, r.display_name AS rider_name,
                               sr.team_id, t.name AS team_name, sr.manufacturer_id, m.name AS manufacturer_name,
                               sr.grid_position, sr.classification_position, sr.classification_status,
                               sr.laps_completed, sr.total_time_ms, sr.points
                        FROM session_results sr
                        JOIN riders r ON r.id = sr.rider_id
                        LEFT JOIN teams t ON t.id = sr.team_id
                        LEFT JOIN manufacturers m ON m.id = sr.manufacturer_id
                        WHERE sr.session_id = :sessionId
                        ORDER BY sr.classification_position NULLS LAST, r.display_name
                        """, params().addValue("sessionId", sessionId), (rs, row) -> new SessionResultView(
                        rs.getObject("id", UUID.class), rs.getObject("session_id", UUID.class),
                        rs.getObject("rider_id", UUID.class), rs.getString("rider_name"),
                        rs.getObject("team_id", UUID.class), rs.getString("team_name"),
                        rs.getObject("manufacturer_id", UUID.class), rs.getString("manufacturer_name"),
                        shortValue(rs.getObject("grid_position")), shortValue(rs.getObject("classification_position")),
                        rs.getString("classification_status"), shortValue(rs.getObject("laps_completed")),
                        (Integer) rs.getObject("total_time_ms"), rs.getBigDecimal("points")));
    }

    public List<LapView> laps(UUID sessionId, UUID riderId, int limit, int offset) {
        return jdbc.query("""
                        SELECT l.id, l.session_result_id, sr.session_id, r.id AS rider_id,
                               r.display_name AS rider_name, l.lap_number, l.lap_time_ms, l.is_valid, l.observed_at
                        FROM rider_laps l
                        JOIN session_results sr ON sr.id = l.session_result_id
                        JOIN riders r ON r.id = sr.rider_id
                        WHERE sr.session_id = :sessionId AND sr.rider_id = :riderId
                        ORDER BY l.lap_number LIMIT :limit OFFSET :offset
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId)
                        .addValue("limit", limit).addValue("offset", offset), (rs, row) -> new LapView(
                        rs.getObject("id", UUID.class), rs.getObject("session_result_id", UUID.class),
                        rs.getObject("session_id", UUID.class), rs.getObject("rider_id", UUID.class),
                        rs.getString("rider_name"), shortValue(rs.getObject("lap_number")),
                        (Integer) rs.getObject("lap_time_ms"), (Boolean) rs.getObject("is_valid"),
                        instant(rs.getTimestamp("observed_at"))));
    }

    public List<PositionView> positions(UUID sessionId, UUID riderId, int limit, int offset) {
        return jdbc.query("""
                        SELECT p.id, p.session_result_id, sr.session_id, r.id AS rider_id,
                               r.display_name AS rider_name, p.lap_number, p.position, p.observed_at
                        FROM race_positions p
                        JOIN session_results sr ON sr.id = p.session_result_id
                        JOIN riders r ON r.id = sr.rider_id
                        WHERE sr.session_id = :sessionId AND sr.rider_id = :riderId
                        ORDER BY p.lap_number LIMIT :limit OFFSET :offset
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId)
                        .addValue("limit", limit).addValue("offset", offset), (rs, row) -> new PositionView(
                        rs.getObject("id", UUID.class), rs.getObject("session_result_id", UUID.class),
                        rs.getObject("session_id", UUID.class), rs.getObject("rider_id", UUID.class),
                        rs.getString("rider_name"), shortValue(rs.getObject("lap_number")),
                        shortValue(rs.getObject("position")), instant(rs.getTimestamp("observed_at"))));
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private Instant instant(java.sql.Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private Short shortValue(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }

    public record SessionView(UUID id, UUID eventId, String sessionType, Instant scheduledStartAt,
                              Instant startedAt, Instant endedAt, String dataMode) {
    }

    public record SessionResultView(UUID id, UUID sessionId, UUID riderId, String riderName,
                                    UUID teamId, String teamName, UUID manufacturerId, String manufacturerName,
                                    Short gridPosition, Short classificationPosition, String classificationStatus,
                                    Short lapsCompleted, Integer totalTimeMs, java.math.BigDecimal points) {
    }

    public record LapView(UUID id, UUID sessionResultId, UUID sessionId, UUID riderId, String riderName,
                          Short lapNumber, Integer lapTimeMs, Boolean valid, Instant observedAt) {
    }

    public record PositionView(UUID id, UUID sessionResultId, UUID sessionId, UUID riderId, String riderName,
                               Short lapNumber, Short position, Instant observedAt) {
    }
}
