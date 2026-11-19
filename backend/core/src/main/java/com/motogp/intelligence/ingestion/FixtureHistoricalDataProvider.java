package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class FixtureHistoricalDataProvider implements HistoricalDataProvider {
    public static final String CODE = "fixture-historical";

    private static final List<ReferenceData> REFERENCE_DATA = List.of(
            new SeasonReference("fixture-season-2025", "fixture-v1", (short) 2025, "MotoGP"),
            new ManufacturerReference("fixture-manufacturer-001", "fixture-v1", "Fixture Works"),
            new TeamReference("fixture-team-001", "fixture-v1", "Fixture Racing", "IT"),
            new RiderReference("fixture-rider-001", "fixture-v1", "Fixture Rider One", "Fixture", "Rider One", "IT"),
            new RiderReference("fixture-rider-002", "fixture-v1", "Fixture Rider Two", "Fixture", "Rider Two", "ES"),
            new CircuitReference("fixture-circuit-mugello", "fixture-v1", "Mugello", "IT", "Europe/Rome",
                    new BigDecimal("43.9975"), new BigDecimal("11.3719")),
            new EventReference("fixture-event-mugello-2025", "fixture-v1", "Italian Grand Prix",
                    "fixture-season-2025", "fixture-circuit-mugello", (short) 6,
                    Instant.parse("2025-06-20T08:00:00Z"), "Europe/Rome"),
            new SessionReference("fixture-session-race-2025", "fixture-v1", "fixture-event-mugello-2025",
                    "RACE", Instant.parse("2025-06-22T12:00:00Z"), Instant.parse("2025-06-22T12:00:00Z"),
                    Instant.parse("2025-06-22T12:05:00Z"), "HISTORICAL"));

    private static final List<ReferenceRelationshipData> RELATIONSHIPS = List.of(
            new TeamManufacturerSeasonReference("fixture-team-manufacturer-2025", "fixture-v1",
                    "fixture-season-2025", "fixture-team-001", "fixture-manufacturer-001",
                    "fixture-event-mugello-2025", null),
            new RiderSeasonEntryReference("fixture-rider-entry-001", "fixture-v1", "fixture-season-2025",
                    "fixture-rider-001", "fixture-team-001", "fixture-manufacturer-001", "MotoGP", (short) 1,
                    "fixture-event-mugello-2025", null),
            new RiderSeasonEntryReference("fixture-rider-entry-002", "fixture-v1", "fixture-season-2025",
                    "fixture-rider-002", "fixture-team-001", "fixture-manufacturer-001", "MotoGP", (short) 2,
                    "fixture-event-mugello-2025", null));

    private static final List<RaceData> RACE_DATA = List.of(
            new SessionResultReference("fixture-result-rider-001", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", "fixture-team-001", "fixture-manufacturer-001", (short) 1, (short) 1,
                    "CLASSIFIED", (short) 3, 276900, new BigDecimal("25.00")),
            new SessionResultReference("fixture-result-rider-002", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", "fixture-team-001", "fixture-manufacturer-001", (short) 2, (short) 2,
                    "CLASSIFIED", (short) 3, 278100, new BigDecimal("20.00")),
            new LapReference("fixture-lap-rider-001-1", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 1, 92300, true, Instant.parse("2025-06-22T12:01:32Z")),
            new LapReference("fixture-lap-rider-001-2", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 2, 92200, true, Instant.parse("2025-06-22T12:03:04Z")),
            new LapReference("fixture-lap-rider-001-3", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 3, 92400, true, Instant.parse("2025-06-22T12:04:36Z")),
            new LapReference("fixture-lap-rider-002-1", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 1, 92700, true, Instant.parse("2025-06-22T12:01:36Z")),
            new LapReference("fixture-lap-rider-002-2", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 2, 92600, true, Instant.parse("2025-06-22T12:03:08Z")),
            new LapReference("fixture-lap-rider-002-3", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 3, 92800, true, Instant.parse("2025-06-22T12:04:40Z")),
            new RacePositionReference("fixture-position-rider-001-0", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 0, (short) 1, Instant.parse("2025-06-22T12:00:00Z")),
            new RacePositionReference("fixture-position-rider-001-1", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 1, (short) 1, Instant.parse("2025-06-22T12:01:32Z")),
            new RacePositionReference("fixture-position-rider-001-2", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 2, (short) 1, Instant.parse("2025-06-22T12:03:04Z")),
            new RacePositionReference("fixture-position-rider-001-3", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-001", (short) 3, (short) 1, Instant.parse("2025-06-22T12:04:36Z")),
            new RacePositionReference("fixture-position-rider-002-0", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 0, (short) 2, Instant.parse("2025-06-22T12:00:00Z")),
            new RacePositionReference("fixture-position-rider-002-1", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 1, (short) 2, Instant.parse("2025-06-22T12:01:36Z")),
            new RacePositionReference("fixture-position-rider-002-2", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 2, (short) 2, Instant.parse("2025-06-22T12:03:08Z")),
            new RacePositionReference("fixture-position-rider-002-3", "fixture-v1", "fixture-session-race-2025",
                    "fixture-rider-002", (short) 3, (short) 2, Instant.parse("2025-06-22T12:04:40Z")));

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ExternalReference> findReferences(ReferenceQuery query) {
        return findReferenceData(query).stream()
                .map(ReferenceData::asExternalReference)
                .toList();
    }

    @Override
    public List<ReferenceData> findReferenceData(ReferenceQuery query) {
        return REFERENCE_DATA.stream()
                .filter(reference -> reference.entityType() == query.entityType())
                .filter(reference -> query.externalId() == null || reference.externalId().equals(query.externalId()))
                .toList();
    }

    @Override
    public List<ReferenceRelationshipData> findReferenceRelationships() {
        return RELATIONSHIPS;
    }

    @Override
    public List<RaceData> findRaceData(RaceDataQuery query) {
        return RACE_DATA.stream()
                .filter(record -> switch (query.type()) {
                    case SESSION_RESULT -> record instanceof SessionResultReference;
                    case RIDER_LAP -> record instanceof LapReference;
                    case RACE_POSITION -> record instanceof RacePositionReference;
                })
                .filter(record -> query.sessionExternalId() == null || sessionExternalId(record).equals(query.sessionExternalId()))
                .toList();
    }

    private String sessionExternalId(RaceData record) {
        return switch (record) {
            case SessionResultReference result -> result.sessionExternalId();
            case LapReference lap -> lap.sessionExternalId();
            case RacePositionReference position -> position.sessionExternalId();
        };
    }
}
