package com.motogp.intelligence.race;

import com.motogp.intelligence.ingestion.FixtureHistoricalDataProvider;
import com.motogp.intelligence.ingestion.RaceDataIngestionService;
import com.motogp.intelligence.ingestion.ReferenceDataIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class HistoricalRaceReadStoreComposeIntegrationTest {
    @Autowired
    private ReferenceDataIngestionService referenceIngestion;

    @Autowired
    private RaceDataIngestionService raceIngestion;

    @Autowired
    private HistoricalRaceReadStore readStore;

    @Test
    void readsHistoricalHierarchyWithStableOrderingAndNullableValues() {
        referenceIngestion.ingest(FixtureHistoricalDataProvider.CODE);
        raceIngestion.ingest(FixtureHistoricalDataProvider.CODE);

        var seasons = readStore.seasons(10, 0);
        assertThat(seasons).hasSize(2);
        UUID seasonId = seasons.get(0).id();
        assertThat(readStore.season(seasonId)).isPresent();

        var events = readStore.events(seasonId, 10, 0);
        assertThat(events).hasSize(2);
        UUID eventId = events.get(0).id();
        assertThat(readStore.event(eventId)).isPresent();

        var sessions = readStore.sessions(eventId, 10, 0);
        assertThat(sessions).isNotEmpty();
        assertThat(sessions).hasSize(3);
        var raceSession = sessions.stream().filter(session -> "RACE".equals(session.sessionType())).findFirst().orElseThrow();
        UUID sessionId = raceSession.id();
        assertThat(readStore.session(sessionId)).isPresent();

        var results = readStore.results(sessionId, null, 10, 0);
        assertThat(results).isNotEmpty();
        UUID riderId = results.get(0).riderId();
        assertThat(readStore.laps(sessionId, riderId, 2, 0)).hasSize(3);
        assertThat(readStore.laps(sessionId, riderId, 10, 0)).hasSize(10)
                .extracting(HistoricalRaceReadStore.LapView::lapNumber).isSorted();
        assertThat(readStore.positions(sessionId, riderId, 2, 0)).hasSize(3);
        assertThat(readStore.positions(sessionId, riderId, 10, 0)).hasSize(11)
                .extracting(HistoricalRaceReadStore.PositionView::lapNumber).isSorted();
        assertThat(readStore.compare(sessionId,
                results.stream().limit(2).map(HistoricalRaceReadStore.SessionResultView::riderId).toList()))
                .isNotEmpty();
    }
}
