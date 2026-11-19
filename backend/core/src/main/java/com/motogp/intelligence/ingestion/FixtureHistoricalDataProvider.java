package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Deterministic synthetic data for local development; it is not official MotoGP history. */
@Component
public class FixtureHistoricalDataProvider implements HistoricalDataProvider {
    public static final String CODE = "fixture-historical";
    public static final String SOURCE_VERSION = "fixture-v1";

    private static final List<SeasonReference> SEASONS = List.of(
            new SeasonReference("fixture-season-2025", SOURCE_VERSION, (short) 2025, "MotoGP"),
            new SeasonReference("fixture-season-2024", SOURCE_VERSION, (short) 2024, "MotoGP"));
    private static final List<ManufacturerReference> MANUFACTURERS = List.of(
            new ManufacturerReference("fixture-manufacturer-001", SOURCE_VERSION, "Fixture Works"),
            new ManufacturerReference("fixture-manufacturer-002", SOURCE_VERSION, "Synthetic Dynamics"));
    private static final List<TeamReference> TEAMS = List.of(
            new TeamReference("fixture-team-001", SOURCE_VERSION, "Fixture Racing", "IT"),
            new TeamReference("fixture-team-002", SOURCE_VERSION, "Fixture Satellite", "ES"),
            new TeamReference("fixture-team-003", SOURCE_VERSION, "Synthetic Dynamics Factory", "JP"),
            new TeamReference("fixture-team-004", SOURCE_VERSION, "Synthetic Dynamics Satellite", "FR"));
    private static final List<RiderReference> RIDERS = List.of(
            new RiderReference("fixture-rider-001", SOURCE_VERSION, "Fixture Rider One", "Fixture", "Rider One", "IT"),
            new RiderReference("fixture-rider-002", SOURCE_VERSION, "Fixture Rider Two", "Fixture", "Rider Two", "ES"),
            new RiderReference("fixture-rider-003", SOURCE_VERSION, "Synthetic Rider Three", "Synthetic", "Rider Three", "FR"),
            new RiderReference("fixture-rider-004", SOURCE_VERSION, "Synthetic Rider Four", "Synthetic", "Rider Four", "DE"),
            new RiderReference("fixture-rider-005", SOURCE_VERSION, "Synthetic Rider Five", "Synthetic", "Rider Five", "GB"),
            new RiderReference("fixture-rider-006", SOURCE_VERSION, "Synthetic Rider Six", "Synthetic", "Rider Six", "JP"),
            new RiderReference("fixture-rider-007", SOURCE_VERSION, "Synthetic Rider Seven", "Synthetic", "Rider Seven", "AU"),
            new RiderReference("fixture-rider-008", SOURCE_VERSION, "Synthetic Rider Eight", "Synthetic", "Rider Eight", "BR"),
            new RiderReference("fixture-rider-009", SOURCE_VERSION, "Synthetic Rider Nine", "Synthetic", "Rider Nine", "NL"),
            new RiderReference("fixture-rider-010", SOURCE_VERSION, "Synthetic Rider Ten", "Synthetic", "Rider Ten", "SE"));
    private static final List<CircuitReference> CIRCUITS = List.of(
            new CircuitReference("fixture-circuit-mugello", SOURCE_VERSION, "Mugello", "IT", "Europe/Rome", new BigDecimal("43.9975"), new BigDecimal("11.3719")),
            new CircuitReference("fixture-circuit-coast", SOURCE_VERSION, "Synthetic Coast Circuit", "ES", "Europe/Madrid", new BigDecimal("39.4699"), new BigDecimal("-0.3763")));
    private static final List<EventReference> EVENTS = List.of(
            new EventReference("fixture-event-mugello-2025", SOURCE_VERSION, "Italian Grand Prix", "fixture-season-2025", "fixture-circuit-mugello", (short) 6, Instant.parse("2025-06-20T08:00:00Z"), "Europe/Rome"),
            new EventReference("fixture-event-coast-2025", SOURCE_VERSION, "Synthetic Coast Grand Prix", "fixture-season-2025", "fixture-circuit-coast", (short) 2, Instant.parse("2025-07-18T08:00:00Z"), "Europe/Madrid"),
            new EventReference("fixture-event-mugello-2024", SOURCE_VERSION, "Fixture Italian Grand Prix 2024", "fixture-season-2024", "fixture-circuit-mugello", (short) 1, Instant.parse("2024-06-21T08:00:00Z"), "Europe/Rome"),
            new EventReference("fixture-event-coast-2024", SOURCE_VERSION, "Synthetic Coast Grand Prix 2024", "fixture-season-2024", "fixture-circuit-coast", (short) 2, Instant.parse("2024-07-19T08:00:00Z"), "Europe/Madrid"));

    private static final List<SessionReference> SESSIONS = buildSessions();
    private static final List<ReferenceData> REFERENCE_DATA = buildReferenceData();
    private static final List<ReferenceRelationshipData> RELATIONSHIPS = buildRelationships();
    private static final List<RaceData> RACE_DATA = buildRaceData();

    @Override public String code() { return CODE; }

    @Override
    public List<ExternalReference> findReferences(ReferenceQuery query) {
        return findReferenceData(query).stream().map(ReferenceData::asExternalReference).toList();
    }

    @Override
    public List<ReferenceData> findReferenceData(ReferenceQuery query) {
        return REFERENCE_DATA.stream().filter(reference -> reference.entityType() == query.entityType())
                .filter(reference -> query.externalId() == null || reference.externalId().equals(query.externalId())).toList();
    }

    @Override public List<ReferenceRelationshipData> findReferenceRelationships() { return RELATIONSHIPS; }

    @Override
    public List<RaceData> findRaceData(RaceDataQuery query) {
        return RACE_DATA.stream().filter(record -> switch (query.type()) {
                    case SESSION_RESULT -> record instanceof SessionResultReference;
                    case RIDER_LAP -> record instanceof LapReference;
                    case RACE_POSITION -> record instanceof RacePositionReference;
                }).filter(record -> query.sessionExternalId() == null || sessionExternalId(record).equals(query.sessionExternalId()))
                .toList();
    }

    private static List<SessionReference> buildSessions() {
        List<SessionReference> sessions = new ArrayList<>();
        for (EventReference event : EVENTS) {
            Instant weekend = event.scheduledStartAt();
            String suffix = event.externalId().substring("fixture-event-".length());
            sessions.add(new SessionReference("fixture-session-practice-" + suffix, SOURCE_VERSION, event.externalId(), "PRACTICE", weekend.plusSeconds(86400), weekend.plusSeconds(86400), weekend.plusSeconds(86400 + 2700), "HISTORICAL"));
            sessions.add(new SessionReference("fixture-session-qualifying-" + suffix, SOURCE_VERSION, event.externalId(), "QUALIFYING", weekend.plusSeconds(172800), weekend.plusSeconds(172800), weekend.plusSeconds(172800 + 1800), "HISTORICAL"));
            String raceId = event.externalId().equals("fixture-event-mugello-2025") ? "fixture-session-race-2025" : "fixture-session-race-" + suffix;
            Instant raceStart = event.externalId().equals("fixture-event-mugello-2025")
                    ? Instant.parse("2025-06-22T12:00:00Z") : weekend.plusSeconds(259200);
            Instant raceEnd = event.externalId().equals("fixture-event-mugello-2025")
                    ? Instant.parse("2025-06-22T12:05:00Z") : raceStart.plusSeconds(1800);
            sessions.add(new SessionReference(raceId, SOURCE_VERSION, event.externalId(), "RACE", raceStart, raceStart, raceEnd, "HISTORICAL"));
        }
        return List.copyOf(sessions);
    }

    private static List<ReferenceData> buildReferenceData() {
        List<ReferenceData> records = new ArrayList<>();
        records.addAll(SEASONS); records.addAll(MANUFACTURERS); records.addAll(CIRCUITS); records.addAll(TEAMS);
        records.addAll(RIDERS); records.addAll(EVENTS); records.addAll(SESSIONS);
        return List.copyOf(records);
    }

    private static List<ReferenceRelationshipData> buildRelationships() {
        List<ReferenceRelationshipData> relationships = new ArrayList<>();
        for (SeasonReference season : SEASONS) {
            String firstEvent = EVENTS.stream().filter(event -> event.seasonExternalId().equals(season.externalId())).findFirst().orElseThrow().externalId();
            for (int team = 1; team <= TEAMS.size(); team++) {
                relationships.add(new TeamManufacturerSeasonReference("fixture-affiliation-" + season.year() + "-" + team, SOURCE_VERSION, season.externalId(), teamId(team), manufacturerForTeam(team), firstEvent, null));
            }
            for (int rider = 1; rider <= RIDERS.size(); rider++) {
                String competitionClass = season.year() == 2025 && rider <= 2 ? "MotoGP" : "MotoGP Synthetic";
                relationships.add(new RiderSeasonEntryReference("fixture-rider-entry-" + season.year() + "-" + rider, SOURCE_VERSION, season.externalId(), riderId(rider), teamId(rider), manufacturerId(rider), competitionClass, (short) rider, firstEvent, null));
            }
        }
        return List.copyOf(relationships);
    }

    private static List<RaceData> buildRaceData() {
        List<RaceData> records = new ArrayList<>();
        for (SessionReference session : SESSIONS) {
            boolean race = session.sessionType().equals("RACE");
            short laps = (short) (race ? 10 : session.sessionType().equals("QUALIFYING") ? 8 : 6);
            int sessionSeed = Math.abs(session.externalId().hashCode() % 170);
            for (int rider = 1; rider <= RIDERS.size(); rider++) {
                short finalPosition = race ? positionFor(rider, laps) : (short) rider;
                int lapTime = 89500 + rider * 210 + sessionSeed;
                String resultExternalId = session.externalId().equals("fixture-session-race-2025") && rider <= 2
                        ? "fixture-result-rider-%03d".formatted(rider)
                        : "fixture-result-" + session.externalId() + "-" + riderId(rider);
                records.add(new SessionResultReference(resultExternalId, SOURCE_VERSION, session.externalId(), riderId(rider), teamId(rider), manufacturerId(rider), (short) rider, finalPosition, "CLASSIFIED", laps, lapTime * laps, race ? BigDecimal.valueOf(Math.max(0, 26 - finalPosition * 3)).setScale(2) : null));
                if (race) {
                    int elapsed = 0;
                    for (short lap = 1; lap <= laps; lap++) {
                        int currentLapTime = lapTime + ((lap + rider) % 5 - 2) * 35;
                        elapsed += currentLapTime;
                        records.add(new LapReference("fixture-lap-" + session.externalId() + "-" + riderId(rider) + "-" + lap, SOURCE_VERSION, session.externalId(), riderId(rider), lap, currentLapTime, true, session.startedAt().plusMillis(elapsed)));
                    }
                    for (short lap = 0; lap <= laps; lap++) {
                        records.add(new RacePositionReference("fixture-position-" + session.externalId() + "-" + riderId(rider) + "-" + lap, SOURCE_VERSION, session.externalId(), riderId(rider), lap, positionFor(rider, lap), session.startedAt().plusSeconds(lap * 90L)));
                    }
                }
            }
        }
        return List.copyOf(records);
    }

    private static short positionFor(int rider, short lap) {
        int shift = (lap >= 4 ? 1 : 0) + (lap >= 8 ? 1 : 0);
        return (short) (((rider - 1 + shift) % RIDERS.size()) + 1);
    }
    private static String riderId(int rider) { return "fixture-rider-%03d".formatted(rider); }
    private static String teamId(int rider) { int team = rider <= 5 ? ((rider - 1) % 2) + 1 : ((rider - 6) % 2) + 3; return "fixture-team-%03d".formatted(team); }
    private static String manufacturerId(int rider) { return rider <= 5 ? "fixture-manufacturer-001" : "fixture-manufacturer-002"; }
    private static String manufacturerForTeam(int team) { return team <= 2 ? "fixture-manufacturer-001" : "fixture-manufacturer-002"; }

    private String sessionExternalId(RaceData record) {
        return switch (record) {
            case SessionResultReference result -> result.sessionExternalId();
            case LapReference lap -> lap.sessionExternalId();
            case RacePositionReference position -> position.sessionExternalId();
        };
    }
}
