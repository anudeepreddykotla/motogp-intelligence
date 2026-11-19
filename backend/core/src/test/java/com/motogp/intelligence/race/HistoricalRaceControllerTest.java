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
                .andExpect(jsonPath("$.data[0].observedAt").value(nullValue()));
    }
}
