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

    public void attachPrimarySourceRecord(String tableName, UUID entityId, UUID sourceRecordId) {
        if (!SetOfReferenceTables.NAMES.contains(tableName)) {
            throw new IllegalArgumentException("Unsupported reference table: " + tableName);
        }
        jdbc.update("UPDATE " + tableName
                        + " SET primary_source_record_id = COALESCE(primary_source_record_id, :sourceRecordId), updated_at = now()"
                        + " WHERE id = :entityId",
                params().addValue("sourceRecordId", sourceRecordId).addValue("entityId", entityId));
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static final class SetOfReferenceTables {
        private static final java.util.Set<String> NAMES = java.util.Set.of(
                "seasons", "manufacturers", "teams", "riders", "circuits", "events");
    }

    public record PersistenceResult(UUID id, boolean created) {
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
}
