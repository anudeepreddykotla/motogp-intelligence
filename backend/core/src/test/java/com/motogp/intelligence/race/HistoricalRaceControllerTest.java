package com.motogp.intelligence.race;

import com.motogp.intelligence.common.CorrelationIdFilter;
import com.motogp.intelligence.common.GlobalExceptionHandler;
import com.motogp.intelligence.system.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@WebMvcTest(HistoricalRaceController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, GlobalExceptionHandler.class})
class HistoricalRaceControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    HistoricalRaceReadStore store;

    @Test
    void returnsLapWithNullObservedAtAsSuccessfulJsonNull() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.laps(sessionId, riderId, 1000, 0)).willReturn(List.of(
                new HistoricalRaceReadStore.LapView(UUID.randomUUID(), UUID.randomUUID(), sessionId, riderId,
                        "Fixture Rider", (short) 1, 92300, true, null)));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/laps", sessionId)
                        .param("riderId", riderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].observedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsFullLapPageAndSignalsAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.laps(sessionId, riderId, 2, 0)).willReturn(List.of(lap(sessionId, riderId, 1),
                lap(sessionId, riderId, 2), lap(sessionId, riderId, 3)));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/laps", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void returnsPartialLapPageWithoutAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.laps(sessionId, riderId, 2, 2)).willReturn(List.of(lap(sessionId, riderId, 3)));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/laps", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsEmptyLapPageWithoutAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.laps(sessionId, riderId, 2, 4)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/laps", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2").param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsFullPositionPageAndSignalsAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.positions(sessionId, riderId, 2, 0)).willReturn(List.of(position(sessionId, riderId, 1),
                position(sessionId, riderId, 2), position(sessionId, riderId, 3)));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/positions", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void returnsPartialPositionPageWithoutAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.positions(sessionId, riderId, 2, 2)).willReturn(List.of(position(sessionId, riderId, 3)));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/positions", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsEmptyPositionPageWithoutAnotherPage() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        given(store.sessionExists(sessionId)).willReturn(true);
        given(store.riderExists(riderId)).willReturn(true);
        given(store.positions(sessionId, riderId, 2, 4)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/positions", sessionId)
                        .param("riderId", riderId.toString()).param("limit", "2").param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsSeasonDetails() throws Exception {
        UUID seasonId = UUID.randomUUID();
        given(store.season(seasonId)).willReturn(Optional.of(
                new HistoricalRaceReadStore.SeasonView(seasonId, (short) 2024, "MotoGP")));

        mockMvc.perform(get("/api/v1/seasons/{seasonId}", seasonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(seasonId.toString()))
                .andExpect(jsonPath("$.data.year").value(2024));
    }

    @Test
    void rejectsInvalidPaginationBeforeQueryingStore() throws Exception {
        mockMvc.perform(get("/api/v1/seasons").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsMalformedSeasonIdAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/seasons/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsMissingRiderIdsAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/riders/compare")
                        .param("sessionId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void keepsUnexpectedExceptionsAsInternalServerError() throws Exception {
        given(store.seasons(100, 0)).willThrow(new IllegalStateException("test failure"));

        mockMvc.perform(get("/api/v1/seasons"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"));
    }

    @Test
    void deniesUnimplementedHistoricalSubEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{sessionId}/replay", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsFullPageAndSignalsAnotherPage() throws Exception {
        given(store.seasons(2, 0)).willReturn(List.of(
                new HistoricalRaceReadStore.SeasonView(UUID.randomUUID(), (short) 2025, "MotoGP"),
                new HistoricalRaceReadStore.SeasonView(UUID.randomUUID(), (short) 2024, "MotoGP"),
                new HistoricalRaceReadStore.SeasonView(UUID.randomUUID(), (short) 2023, "MotoGP")));

        mockMvc.perform(get("/api/v1/seasons").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void returnsPartialFinalPageWithoutAnotherPage() throws Exception {
        given(store.seasons(2, 2)).willReturn(List.of(
                new HistoricalRaceReadStore.SeasonView(UUID.randomUUID(), (short) 2023, "MotoGP")));

        mockMvc.perform(get("/api/v1/seasons").param("limit", "2").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsEmptyPageWithoutAnotherPage() throws Exception {
        given(store.seasons(2, 4)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/seasons").param("limit", "2").param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.limit").value(2))
                .andExpect(jsonPath("$.data.offset").value(4))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void returnsEventsFilteredBySeason() throws Exception {
        UUID seasonId = UUID.randomUUID();
        given(store.seasonExists(seasonId)).willReturn(true);
        given(store.events(seasonId, 100, 0)).willReturn(List.of(
                new HistoricalRaceReadStore.EventView(UUID.randomUUID(), seasonId, (short) 2024, "MotoGP",
                        UUID.randomUUID(), "Fixture Circuit", "ES", "Fixture GP", (short) 1, null,
                        "Europe/Madrid")));

        mockMvc.perform(get("/api/v1/events").param("seasonId", seasonId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Fixture GP"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void rejectsComparisonWithFewerThanTwoRiders() throws Exception {
        mockMvc.perform(get("/api/v1/riders/compare")
                        .param("sessionId", UUID.randomUUID().toString())
                        .param("riderIds", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    private HistoricalRaceReadStore.LapView lap(UUID sessionId, UUID riderId, int lapNumber) {
        return new HistoricalRaceReadStore.LapView(UUID.randomUUID(), UUID.randomUUID(), sessionId, riderId,
                "Fixture Rider", (short) lapNumber, 92300, true, null);
    }

    private HistoricalRaceReadStore.PositionView position(UUID sessionId, UUID riderId, int lapNumber) {
        return new HistoricalRaceReadStore.PositionView(UUID.randomUUID(), UUID.randomUUID(), sessionId, riderId,
                "Fixture Rider", (short) lapNumber, (short) 1, null);
    }
}
