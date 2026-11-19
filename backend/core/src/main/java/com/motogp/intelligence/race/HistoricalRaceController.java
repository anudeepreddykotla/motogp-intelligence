package com.motogp.intelligence.race;

import com.motogp.intelligence.common.ApiResponse;
import com.motogp.intelligence.common.PaginatedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class HistoricalRaceController {
    private static final int DEFAULT_LIST_LIMIT = 100;
    private static final int DEFAULT_RESULT_LIMIT = 500;
    private static final int MAX_LIST_LIMIT = 500;
    private static final int MAX_RESULT_LIMIT = 5000;
    private static final Set<String> CLASSIFICATION_STATUSES =
            Set.of("CLASSIFIED", "DNF", "DNS", "DSQ", "OTHER");

    private final HistoricalRaceReadStore store;

    public HistoricalRaceController(HistoricalRaceReadStore store) {
        this.store = store;
    }

    @GetMapping("/seasons")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.SeasonView>> seasons(
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIST_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return paginated(store.seasons(limit, offset), limit, offset);
    }

    @GetMapping("/seasons/{seasonId}")
    public ApiResponse<HistoricalRaceReadStore.SeasonView> season(@PathVariable UUID seasonId) {
        return ApiResponse.success(requestId(), store.season(seasonId)
                .orElseThrow(() -> new HistoricalResourceNotFoundException("season", seasonId)));
    }

    @GetMapping("/events")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.EventView>> events(
            @RequestParam(required = false) UUID seasonId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIST_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        if (seasonId != null && !store.seasonExists(seasonId)) {
            throw new HistoricalResourceNotFoundException("season", seasonId);
        }
        return paginated(store.events(seasonId, limit, offset), limit, offset);
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<HistoricalRaceReadStore.EventView> event(@PathVariable UUID eventId) {
        return ApiResponse.success(requestId(), store.event(eventId)
                .orElseThrow(() -> new HistoricalResourceNotFoundException("event", eventId)));
    }

    @GetMapping("/events/{eventId}/sessions")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.SessionView>> sessions(@PathVariable UUID eventId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIST_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireEvent(eventId);
        return paginated(store.sessions(eventId, limit, offset), limit, offset);
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<HistoricalRaceReadStore.SessionView> session(@PathVariable UUID sessionId) {
        return ApiResponse.success(requestId(), store.session(sessionId)
                .orElseThrow(() -> new HistoricalResourceNotFoundException("session", sessionId)));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.SessionResultView>> results(@PathVariable UUID sessionId,
            @RequestParam(required = false) String classificationStatus,
            @RequestParam(defaultValue = "500") @Min(1) @Max(MAX_RESULT_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireSession(sessionId);
        if (classificationStatus != null && !CLASSIFICATION_STATUSES.contains(classificationStatus)) {
            throw new InvalidHistoricalQueryException("Unsupported classificationStatus: " + classificationStatus);
        }
        return paginated(store.results(sessionId, classificationStatus, limit, offset), limit, offset);
    }

    @GetMapping("/riders/compare")
    public ApiResponse<List<HistoricalRaceReadStore.ComparisonView>> compare(
            @RequestParam UUID sessionId,
            @RequestParam @NotNull @Size(min = 2, max = 4) List<UUID> riderIds) {
        requireSession(sessionId);
        if (riderIds.stream().distinct().count() != riderIds.size()) {
            throw new InvalidHistoricalQueryException("riderIds must contain distinct riders");
        }
        riderIds.forEach(this::requireRider);
        return ApiResponse.success(requestId(), store.compare(sessionId, riderIds));
    }

    @GetMapping("/sessions/{sessionId}/laps")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.LapView>> laps(@PathVariable UUID sessionId,
            @RequestParam @NotNull UUID riderId,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(MAX_RESULT_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireSessionAndRider(sessionId, riderId);
        return paginated(store.laps(sessionId, riderId, limit, offset), limit, offset);
    }

    @GetMapping("/sessions/{sessionId}/positions")
    public ApiResponse<PaginatedResponse<HistoricalRaceReadStore.PositionView>> positions(@PathVariable UUID sessionId,
            @RequestParam @NotNull UUID riderId,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(MAX_RESULT_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        requireSessionAndRider(sessionId, riderId);
        return paginated(store.positions(sessionId, riderId, limit, offset), limit, offset);
    }

    private void requireEvent(UUID eventId) {
        if (!store.eventExists(eventId)) throw new HistoricalResourceNotFoundException("event", eventId);
    }

    private void requireSession(UUID sessionId) {
        if (!store.sessionExists(sessionId)) throw new HistoricalResourceNotFoundException("session", sessionId);
    }

    private void requireRider(UUID riderId) {
        if (!store.riderExists(riderId)) throw new HistoricalResourceNotFoundException("rider", riderId);
    }

    private void requireSessionAndRider(UUID sessionId, UUID riderId) {
        requireSession(sessionId);
        requireRider(riderId);
    }

    private <T> ApiResponse<PaginatedResponse<T>> paginated(List<T> rows, int limit, int offset) {
        boolean hasNext = rows.size() > limit;
        List<T> items = hasNext ? rows.subList(0, limit) : rows;
        return ApiResponse.success(requestId(), new PaginatedResponse<>(items, limit, offset, hasNext));
    }

    private String requestId() {
        return MDC.get(com.motogp.intelligence.common.CorrelationIdFilter.MDC_KEY);
    }
}
