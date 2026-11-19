package com.motogp.intelligence.ingestion;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReferenceDataStore {
    private final NamedParameterJdbcTemplate jdbc;

    public ReferenceDataStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SeasonRow> findSeason(short year, String championship) {
        return jdbc.query("SELECT id, year, championship FROM seasons WHERE year = :year AND championship = :championship",
                params().addValue("year", year).addValue("championship", championship),
                rows -> rows.next() ? Optional.of(new SeasonRow(rows.getObject("id", UUID.class),
                        rows.getShort("year"), rows.getString("championship"))) : Optional.empty());
    }

    public UUID insertSeason(SeasonReference data) {
        return jdbc.queryForObject("""
                        INSERT INTO seasons (year, championship) VALUES (:year, :championship) RETURNING id
                        """, params().addValue("year", data.year()).addValue("championship", data.championship()), UUID.class);
    }

    public Optional<ManufacturerRow> findManufacturer(String name) {
        return jdbc.query("SELECT id, name FROM manufacturers WHERE name = :name",
                params().addValue("name", name),
                rows -> rows.next() ? Optional.of(new ManufacturerRow(rows.getObject("id", UUID.class),
                        rows.getString("name"))) : Optional.empty());
    }

    public UUID insertManufacturer(ManufacturerReference data) {
        return jdbc.queryForObject("INSERT INTO manufacturers (name) VALUES (:name) RETURNING id",
                params().addValue("name", data.name()), UUID.class);
    }

    public Optional<TeamRow> findTeam(String name, String countryCode) {
        return jdbc.query("SELECT id, name, country_code FROM teams WHERE name = :name AND country_code IS NOT DISTINCT FROM :countryCode",
                params().addValue("name", name).addValue("countryCode", countryCode),
                rows -> rows.next() ? Optional.of(new TeamRow(rows.getObject("id", UUID.class), rows.getString("name"),
                        rows.getString("country_code"))) : Optional.empty());
    }

    public UUID insertTeam(TeamReference data) {
        return jdbc.queryForObject("INSERT INTO teams (name, country_code) VALUES (:name, :countryCode) RETURNING id",
                params().addValue("name", data.name()).addValue("countryCode", data.countryCode()), UUID.class);
    }

    public Optional<RiderRow> findRider(String displayName) {
        return jdbc.query("SELECT id, display_name, given_name, family_name, nationality_code FROM riders WHERE display_name = :displayName",
                params().addValue("displayName", displayName),
                rows -> rows.next() ? Optional.of(new RiderRow(rows.getObject("id", UUID.class),
                        rows.getString("display_name"), rows.getString("given_name"), rows.getString("family_name"),
                        rows.getString("nationality_code"))) : Optional.empty());
    }

    public UUID insertRider(RiderReference data) {
        return jdbc.queryForObject("""
                        INSERT INTO riders (display_name, given_name, family_name, nationality_code)
                        VALUES (:displayName, :givenName, :familyName, :nationalityCode) RETURNING id
                        """, params().addValue("displayName", data.displayName()).addValue("givenName", data.givenName())
                        .addValue("familyName", data.familyName()).addValue("nationalityCode", data.nationalityCode()), UUID.class);
    }

    public Optional<CircuitRow> findCircuit(String name, String countryCode) {
        return jdbc.query("""
                        SELECT id, name, country_code, timezone, latitude, longitude
                        FROM circuits WHERE name = :name AND country_code IS NOT DISTINCT FROM :countryCode
                        """, params().addValue("name", name).addValue("countryCode", countryCode),
                rows -> rows.next() ? Optional.of(new CircuitRow(rows.getObject("id", UUID.class), rows.getString("name"),
                        rows.getString("country_code"), rows.getString("timezone"), rows.getBigDecimal("latitude"),
                        rows.getBigDecimal("longitude"))) : Optional.empty());
    }

    public UUID insertCircuit(CircuitReference data) {
        return jdbc.queryForObject("""
                        INSERT INTO circuits (name, country_code, timezone, latitude, longitude)
                        VALUES (:name, :countryCode, :timezone, :latitude, :longitude) RETURNING id
                        """, params().addValue("name", data.name()).addValue("countryCode", data.countryCode())
                        .addValue("timezone", data.timezone()).addValue("latitude", data.latitude())
                        .addValue("longitude", data.longitude()), UUID.class);
    }

    public Optional<EventRow> findEvent(UUID seasonId, String name) {
        return jdbc.query("""
                        SELECT id, season_id, circuit_id, name, round_number, scheduled_start_at, timezone
                        FROM events WHERE season_id = :seasonId AND name = :name
                        """, params().addValue("seasonId", seasonId).addValue("name", name),
                rows -> rows.next() ? Optional.of(new EventRow(rows.getObject("id", UUID.class),
                        rows.getObject("season_id", UUID.class), rows.getObject("circuit_id", UUID.class),
                        rows.getString("name"), rows.getShort("round_number"),
                        toInstant(rows.getTimestamp("scheduled_start_at")), rows.getString("timezone"))) : Optional.empty());
    }

    public UUID insertEvent(EventReference data, UUID seasonId, UUID circuitId) {
        return jdbc.queryForObject("""
                        INSERT INTO events (season_id, circuit_id, name, round_number, scheduled_start_at, timezone)
                        VALUES (:seasonId, :circuitId, :name, :roundNumber, :scheduledStartAt, :timezone) RETURNING id
                        """, params().addValue("seasonId", seasonId).addValue("circuitId", circuitId)
                        .addValue("name", data.name()).addValue("roundNumber", data.roundNumber())
                        .addValue("scheduledStartAt", data.scheduledStartAt() == null ? null : Timestamp.from(data.scheduledStartAt()))
                        .addValue("timezone", data.timezone()), UUID.class);
    }

    public Optional<SessionRow> findSession(UUID eventId, String sessionType) {
        return jdbc.query("""
                        SELECT id, event_id, session_type, scheduled_start_at, started_at, ended_at, data_mode
                        FROM sessions WHERE event_id = :eventId AND session_type = :sessionType
                        """, params().addValue("eventId", eventId).addValue("sessionType", sessionType),
                rows -> rows.next() ? Optional.of(new SessionRow(rows.getObject("id", UUID.class),
                        rows.getObject("event_id", UUID.class), rows.getString("session_type"),
                        toInstant(rows.getTimestamp("scheduled_start_at")), toInstant(rows.getTimestamp("started_at")),
                        toInstant(rows.getTimestamp("ended_at")), rows.getString("data_mode"))) : Optional.empty());
    }

    public UUID insertSession(SessionReference data, UUID eventId) {
        return jdbc.queryForObject("""
                        INSERT INTO sessions (event_id, session_type, scheduled_start_at, started_at, ended_at, data_mode)
                        VALUES (:eventId, :sessionType, :scheduledStartAt, :startedAt, :endedAt, :dataMode) RETURNING id
                        """, params().addValue("eventId", eventId).addValue("sessionType", data.sessionType())
                        .addValue("scheduledStartAt", timestamp(data.scheduledStartAt()))
                        .addValue("startedAt", timestamp(data.startedAt())).addValue("endedAt", timestamp(data.endedAt()))
                        .addValue("dataMode", data.dataMode()), UUID.class);
    }

    public void attachPrimarySourceRecord(String tableName, UUID entityId, UUID sourceRecordId) {
        if (!SetOfReferenceTables.NAMES.contains(tableName)) {
            throw new IllegalArgumentException("Unsupported reference table: " + tableName);
        }
        String updatedAt = SetOfReferenceTables.WITHOUT_UPDATED_AT.contains(tableName) ? "" : ", updated_at = now()";
        jdbc.update("UPDATE " + tableName
                        + " SET primary_source_record_id = COALESCE(primary_source_record_id, :sourceRecordId)" + updatedAt
                        + " WHERE id = :entityId",
                params().addValue("sourceRecordId", sourceRecordId).addValue("entityId", entityId));
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static final class SetOfReferenceTables {
        private static final java.util.Set<String> NAMES = java.util.Set.of(
                "seasons", "manufacturers", "teams", "riders", "circuits", "events", "sessions",
                "team_manufacturer_seasons", "rider_season_entries");
        private static final java.util.Set<String> WITHOUT_UPDATED_AT = java.util.Set.of(
                "team_manufacturer_seasons", "rider_season_entries");
    }

    public Optional<TeamManufacturerSeasonRow> findTeamManufacturerSeason(UUID seasonId, UUID teamId, UUID manufacturerId, UUID fromEventId) {
        return jdbc.query("SELECT id, season_id, team_id, manufacturer_id, valid_from_event_id, valid_to_event_id FROM team_manufacturer_seasons WHERE season_id=:seasonId AND team_id=:teamId "
                        + "AND manufacturer_id=:manufacturerId AND valid_from_event_id IS NOT DISTINCT FROM :fromEventId",
                params().addValue("seasonId", seasonId).addValue("teamId", teamId)
                        .addValue("manufacturerId", manufacturerId).addValue("fromEventId", fromEventId),
                rows -> rows.next() ? Optional.of(new TeamManufacturerSeasonRow(rows.getObject("id", UUID.class),
                        rows.getObject("season_id", UUID.class), rows.getObject("team_id", UUID.class),
                        rows.getObject("manufacturer_id", UUID.class), rows.getObject("valid_from_event_id", UUID.class),
                        rows.getObject("valid_to_event_id", UUID.class))) : Optional.empty());
    }

    public UUID insertTeamManufacturerSeason(TeamManufacturerSeasonReference data, UUID seasonId, UUID teamId,
                                             UUID manufacturerId, UUID fromEventId, UUID toEventId) {
        return jdbc.queryForObject("""
                INSERT INTO team_manufacturer_seasons (season_id, team_id, manufacturer_id, valid_from_event_id, valid_to_event_id)
                VALUES (:seasonId,:teamId,:manufacturerId,:fromEventId,:toEventId) RETURNING id
                """, params().addValue("seasonId", seasonId).addValue("teamId", teamId)
                .addValue("manufacturerId", manufacturerId).addValue("fromEventId", fromEventId)
                .addValue("toEventId", toEventId), UUID.class);
    }

    public Optional<RiderSeasonEntryRow> findRiderSeasonEntry(UUID seasonId, UUID riderId, UUID fromEventId) {
        return jdbc.query("SELECT id, season_id, rider_id, team_id, manufacturer_id, competition_class, racing_number, valid_from_event_id, valid_to_event_id FROM rider_season_entries WHERE season_id=:seasonId AND rider_id=:riderId "
                        + "AND valid_from_event_id IS NOT DISTINCT FROM :fromEventId",
                params().addValue("seasonId", seasonId).addValue("riderId", riderId).addValue("fromEventId", fromEventId),
                rows -> rows.next() ? Optional.of(new RiderSeasonEntryRow(rows.getObject("id", UUID.class),
                        rows.getObject("season_id", UUID.class), rows.getObject("rider_id", UUID.class),
                        rows.getObject("team_id", UUID.class), rows.getObject("manufacturer_id", UUID.class),
                        rows.getString("competition_class"), rows.getObject("racing_number", Short.class),
                        rows.getObject("valid_from_event_id", UUID.class), rows.getObject("valid_to_event_id", UUID.class))) : Optional.empty());
    }

    public UUID insertRiderSeasonEntry(RiderSeasonEntryReference data, UUID seasonId, UUID riderId, UUID teamId,
                                       UUID manufacturerId, UUID fromEventId, UUID toEventId) {
        return jdbc.queryForObject("""
                INSERT INTO rider_season_entries (season_id, rider_id, team_id, manufacturer_id, competition_class,
                    racing_number, valid_from_event_id, valid_to_event_id)
                VALUES (:seasonId,:riderId,:teamId,:manufacturerId,:competitionClass,:racingNumber,:fromEventId,:toEventId)
                RETURNING id
                """, params().addValue("seasonId", seasonId).addValue("riderId", riderId)
                .addValue("teamId", teamId).addValue("manufacturerId", manufacturerId)
                .addValue("competitionClass", data.competitionClass()).addValue("racingNumber", data.racingNumber())
                .addValue("fromEventId", fromEventId).addValue("toEventId", toEventId), UUID.class);
    }

    public record PersistenceResult(UUID id, boolean created) {
    }

    public record TeamManufacturerSeasonRow(UUID id, UUID seasonId, UUID teamId, UUID manufacturerId,
                                            UUID validFromEventId, UUID validToEventId) {
    }

    public record RiderSeasonEntryRow(UUID id, UUID seasonId, UUID riderId, UUID teamId, UUID manufacturerId,
                                      String competitionClass, Short racingNumber, UUID validFromEventId,
                                      UUID validToEventId) {
    }

    public record SeasonRow(UUID id, short year, String championship) {
    }

    public record ManufacturerRow(UUID id, String name) {
    }

    public record TeamRow(UUID id, String name, String countryCode) {
    }

    public record RiderRow(UUID id, String displayName, String givenName, String familyName, String nationalityCode) {
    }

    public record CircuitRow(UUID id, String name, String countryCode, String timezone,
                             BigDecimal latitude, BigDecimal longitude) {
    }

    public record EventRow(UUID id, UUID seasonId, UUID circuitId, String name, short roundNumber,
                           Instant scheduledStartAt, String timezone) {
    }

    public record SessionRow(UUID id, UUID eventId, String sessionType, Instant scheduledStartAt,
                             Instant startedAt, Instant endedAt, String dataMode) {
    }
}
