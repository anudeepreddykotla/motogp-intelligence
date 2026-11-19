package com.motogp.intelligence.race;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HistoricalRaceReadStore {
    private final NamedParameterJdbcTemplate jdbc;

    public HistoricalRaceReadStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<SeasonView> seasons(int limit, int offset) {
        return jdbc.query("""
                        SELECT id, year, championship FROM seasons
                        ORDER BY year DESC, championship, id LIMIT :limit OFFSET :offset
                        """, params().addValue("limit", limit + 1).addValue("offset", offset),
                (rs, row) -> new SeasonView(rs.getObject("id", UUID.class), shortValue(rs.getObject("year")),
                        rs.getString("championship")));
    }

    public Optional<SeasonView> season(UUID seasonId) {
        return queryOptional("SELECT id, year, championship FROM seasons WHERE id = :seasonId",
                params().addValue("seasonId", seasonId), (rs, row) -> new SeasonView(
                        rs.getObject("id", UUID.class), shortValue(rs.getObject("year")),
                        rs.getString("championship")));
    }

    public List<EventView> events(UUID seasonId, int limit, int offset) {
        return jdbc.query("""
                        SELECT e.id, e.season_id, s.year AS season_year, s.championship,
                               e.circuit_id, c.name AS circuit_name, c.country_code,
                               e.name, e.round_number, e.scheduled_start_at, e.timezone
                        FROM events e JOIN seasons s ON s.id = e.season_id
                        JOIN circuits c ON c.id = e.circuit_id
                        WHERE (CAST(:seasonId AS uuid) IS NULL OR e.season_id = :seasonId)
                        ORDER BY s.year DESC, e.round_number, e.name, e.id
                        LIMIT :limit OFFSET :offset
                        """, params().addValue("seasonId", seasonId).addValue("limit", limit + 1)
                        .addValue("offset", offset), (rs, row) -> eventView(rs, row));
    }

    public Optional<EventView> event(UUID eventId) {
        return queryOptional("""
                SELECT e.id, e.season_id, s.year AS season_year, s.championship,
                       e.circuit_id, c.name AS circuit_name, c.country_code,
                       e.name, e.round_number, e.scheduled_start_at, e.timezone
                FROM events e JOIN seasons s ON s.id = e.season_id
                JOIN circuits c ON c.id = e.circuit_id WHERE e.id = :eventId
                """, params().addValue("eventId", eventId), (rs, row) -> eventView(rs, row));
    }

    public List<SessionView> sessions(UUID eventId, int limit, int offset) {
        return jdbc.query("""
                        SELECT s.id, s.event_id, e.name AS event_name, e.season_id,
                               s.session_type, s.scheduled_start_at, s.started_at, s.ended_at, s.data_mode
                        FROM sessions s JOIN events e ON e.id = s.event_id
                        WHERE s.event_id = :eventId
                        ORDER BY s.scheduled_start_at NULLS LAST, s.session_type, s.id
                        LIMIT :limit OFFSET :offset
                        """, params().addValue("eventId", eventId).addValue("limit", limit + 1)
                        .addValue("offset", offset), (rs, row) -> sessionView(rs, row));
    }

    public Optional<SessionView> session(UUID sessionId) {
        return queryOptional("""
                SELECT s.id, s.event_id, e.name AS event_name, e.season_id,
                       s.session_type, s.scheduled_start_at, s.started_at, s.ended_at, s.data_mode
                FROM sessions s JOIN events e ON e.id = s.event_id WHERE s.id = :sessionId
                """, params().addValue("sessionId", sessionId), (rs, row) -> sessionView(rs, row));
    }

    public boolean seasonExists(UUID seasonId) { return exists("seasons", seasonId); }
    public boolean eventExists(UUID eventId) { return exists("events", eventId); }
    public boolean sessionExists(UUID sessionId) { return exists("sessions", sessionId); }
    public boolean riderExists(UUID riderId) { return exists("riders", riderId); }

    public List<SessionResultView> results(UUID sessionId, String classificationStatus, int limit, int offset) {
        return jdbc.query("""
                        SELECT sr.id, sr.session_id, sr.rider_id, r.display_name AS rider_name,
                               sr.team_id, t.name AS team_name, sr.manufacturer_id, m.name AS manufacturer_name,
                               sr.grid_position, sr.classification_position, sr.classification_status,
                               sr.laps_completed, sr.total_time_ms, sr.points
                        FROM session_results sr JOIN riders r ON r.id = sr.rider_id
                        LEFT JOIN teams t ON t.id = sr.team_id
                        LEFT JOIN manufacturers m ON m.id = sr.manufacturer_id
                        WHERE sr.session_id = :sessionId
                          AND (CAST(:classificationStatus AS text) IS NULL
                               OR sr.classification_status = :classificationStatus)
                        ORDER BY sr.classification_position NULLS LAST, r.display_name, sr.id
                        LIMIT :limit OFFSET :offset
                        """, params().addValue("sessionId", sessionId)
                        .addValue("classificationStatus", classificationStatus).addValue("limit", limit + 1)
                        .addValue("offset", offset), (rs, row) -> sessionResultView(rs, row));
    }

    public List<ComparisonView> compare(UUID sessionId, List<UUID> riderIds) {
        return jdbc.query("""
                        SELECT sr.session_id, sr.rider_id, r.display_name AS rider_name,
                               sr.classification_position, sr.classification_status,
                               sr.laps_completed, sr.total_time_ms, sr.points
                        FROM session_results sr JOIN riders r ON r.id = sr.rider_id
                        WHERE sr.session_id = :sessionId AND sr.rider_id IN (:riderIds)
                        ORDER BY sr.classification_position NULLS LAST, r.display_name, sr.rider_id
                        """, params().addValue("sessionId", sessionId).addValue("riderIds", riderIds),
                (rs, row) -> new ComparisonView(rs.getObject("session_id", UUID.class),
                        rs.getObject("rider_id", UUID.class), rs.getString("rider_name"),
                        shortValue(rs.getObject("classification_position")),
                        rs.getString("classification_status"), shortValue(rs.getObject("laps_completed")),
                        (Integer) rs.getObject("total_time_ms"), rs.getBigDecimal("points")));
    }

    public List<LapView> laps(UUID sessionId, UUID riderId, int limit, int offset) {
        return jdbc.query("""
                        SELECT l.id, l.session_result_id, sr.session_id, r.id AS rider_id,
                               r.display_name AS rider_name, l.lap_number, l.lap_time_ms, l.is_valid, l.observed_at
                        FROM rider_laps l JOIN session_results sr ON sr.id = l.session_result_id
                        JOIN riders r ON r.id = sr.rider_id
                        WHERE sr.session_id = :sessionId AND sr.rider_id = :riderId
                        ORDER BY l.lap_number, l.id LIMIT :limit OFFSET :offset
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId)
                        .addValue("limit", limit + 1).addValue("offset", offset), (rs, row) -> new LapView(
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
                        FROM race_positions p JOIN session_results sr ON sr.id = p.session_result_id
                        JOIN riders r ON r.id = sr.rider_id
                        WHERE sr.session_id = :sessionId AND sr.rider_id = :riderId
                        ORDER BY p.lap_number, p.id LIMIT :limit OFFSET :offset
                        """, params().addValue("sessionId", sessionId).addValue("riderId", riderId)
                        .addValue("limit", limit + 1).addValue("offset", offset), (rs, row) -> new PositionView(
                        rs.getObject("id", UUID.class), rs.getObject("session_result_id", UUID.class),
                        rs.getObject("session_id", UUID.class), rs.getObject("rider_id", UUID.class),
                        rs.getString("rider_name"), shortValue(rs.getObject("lap_number")),
                        shortValue(rs.getObject("position")), instant(rs.getTimestamp("observed_at"))));
    }

    private EventView eventView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new EventView(rs.getObject("id", UUID.class), rs.getObject("season_id", UUID.class),
                shortValue(rs.getObject("season_year")), rs.getString("championship"),
                rs.getObject("circuit_id", UUID.class), rs.getString("circuit_name"),
                rs.getString("country_code"), rs.getString("name"), shortValue(rs.getObject("round_number")),
                instant(rs.getTimestamp("scheduled_start_at")), rs.getString("timezone"));
    }

    private SessionView sessionView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new SessionView(rs.getObject("id", UUID.class), rs.getObject("event_id", UUID.class),
                rs.getObject("season_id", UUID.class), rs.getString("event_name"), rs.getString("session_type"),
                instant(rs.getTimestamp("scheduled_start_at")), instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("ended_at")), rs.getString("data_mode"));
    }

    private SessionResultView sessionResultView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new SessionResultView(rs.getObject("id", UUID.class), rs.getObject("session_id", UUID.class),
                rs.getObject("rider_id", UUID.class), rs.getString("rider_name"),
                rs.getObject("team_id", UUID.class), rs.getString("team_name"),
                rs.getObject("manufacturer_id", UUID.class), rs.getString("manufacturer_name"),
                shortValue(rs.getObject("grid_position")), shortValue(rs.getObject("classification_position")),
                rs.getString("classification_status"), shortValue(rs.getObject("laps_completed")),
                (Integer) rs.getObject("total_time_ms"), rs.getBigDecimal("points"));
    }

    private <T> Optional<T> queryOptional(String sql, MapSqlParameterSource parameters,
                                           org.springframework.jdbc.core.RowMapper<T> mapper) {
        return jdbc.query(sql, parameters, mapper).stream().findFirst();
    }

    private boolean exists(String table, UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id=:id)",
                params().addValue("id", id), Boolean.class));
    }

    private MapSqlParameterSource params() { return new MapSqlParameterSource(); }
    private Instant instant(java.sql.Timestamp value) { return value == null ? null : value.toInstant(); }
    private Short shortValue(Object value) { return value == null ? null : ((Number) value).shortValue(); }

    public record SeasonView(UUID id, Short year, String championship) {}
    public record EventView(UUID id, UUID seasonId, Short seasonYear, String championship, UUID circuitId,
                            String circuitName, String countryCode, String name, Short roundNumber,
                            Instant scheduledStartAt, String timezone) {}
    public record SessionView(UUID id, UUID eventId, UUID seasonId, String eventName, String sessionType,
                              Instant scheduledStartAt, Instant startedAt, Instant endedAt, String dataMode) {}
    public record SessionResultView(UUID id, UUID sessionId, UUID riderId, String riderName,
                                    UUID teamId, String teamName, UUID manufacturerId, String manufacturerName,
                                    Short gridPosition, Short classificationPosition, String classificationStatus,
                                    Short lapsCompleted, Integer totalTimeMs, java.math.BigDecimal points) {}
    public record ComparisonView(UUID sessionId, UUID riderId, String riderName, Short classificationPosition,
                                 String classificationStatus, Short lapsCompleted, Integer totalTimeMs,
                                 java.math.BigDecimal points) {}
    public record LapView(UUID id, UUID sessionResultId, UUID sessionId, UUID riderId, String riderName,
                          Short lapNumber, Integer lapTimeMs, Boolean valid, Instant observedAt) {}
    public record PositionView(UUID id, UUID sessionResultId, UUID sessionId, UUID riderId, String riderName,
                               Short lapNumber, Short position, Instant observedAt) {}
}
