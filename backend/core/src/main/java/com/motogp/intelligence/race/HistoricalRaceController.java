package com.motogp.intelligence.race;

import com.motogp.intelligence.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class HistoricalRaceController {
    private final HistoricalRaceReadStore store;

    public HistoricalRaceController(HistoricalRaceReadStore store) {
        this.store = store;
    }

    @GetMapping("/events/{eventId}/sessions")
    public ApiResponse<List<HistoricalRaceReadStore.SessionView>> sessions(@PathVariable UUID eventId) {
        if (!store.eventExists(eventId)) throw new HistoricalResourceNotFoundException("event", eventId);
        return ApiResponse.success(requestId(), store.sessions(eventId));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ApiResponse<List<HistoricalRaceReadStore.SessionResultView>> results(@PathVariable UUID sessionId) {
        if (!store.sessionExists(sessionId)) throw new HistoricalResourceNotFoundException("session", sessionId);
        return ApiResponse.success(requestId(), store.results(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/laps")
    public ApiResponse<List<HistoricalRaceReadStore.LapView>> laps(@PathVariable UUID sessionId,
                                                                    @RequestParam @NotNull UUID riderId,
                                                                    @RequestParam(defaultValue = "1000") @Min(1) @Max(5000) int limit,
                                                                    @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireSessionAndRider(sessionId, riderId);
        return ApiResponse.success(requestId(), store.laps(sessionId, riderId, limit, offset));
    }

    @GetMapping("/sessions/{sessionId}/positions")
    public ApiResponse<List<HistoricalRaceReadStore.PositionView>> positions(@PathVariable UUID sessionId,
                                                                               @RequestParam @NotNull UUID riderId,
                                                                               @RequestParam(defaultValue = "1000") @Min(1) @Max(5000) int limit,
                                                                               @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireSessionAndRider(sessionId, riderId);
        return ApiResponse.success(requestId(), store.positions(sessionId, riderId, limit, offset));
    }

    private void requireSessionAndRider(UUID sessionId, UUID riderId) {
        if (!store.sessionExists(sessionId)) throw new HistoricalResourceNotFoundException("session", sessionId);
        if (!store.riderExists(riderId)) throw new HistoricalResourceNotFoundException("rider", riderId);
    }

    private String requestId() {
        return MDC.get(com.motogp.intelligence.common.CorrelationIdFilter.MDC_KEY);
    }
}
