package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RaceDataIngestionServiceTest {
    private static final String PROVIDER = FixtureHistoricalDataProvider.CODE;
    private static final UUID PROVIDER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID RIDER_ID = UUID.randomUUID();
    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID MANUFACTURER_ID = UUID.randomUUID();

    @Mock ProviderAdapterRegistry providers;
    @Mock IdentityMappingStore identityStore;
    @Mock IngestionRunStore runs;
    @Mock RaceDataPersistenceService persistence;

    private RaceDataIngestionService service;

    @BeforeEach
    void setUp() {
        service = new RaceDataIngestionService(providers, identityStore, runs, persistence);
        lenient().when(providers.require(PROVIDER)).thenReturn(new FixtureHistoricalDataProvider());
        lenient().when(identityStore.providerId(PROVIDER)).thenReturn(PROVIDER_ID);
        lenient().when(runs.ensureHistoricalJob(PROVIDER)).thenReturn(UUID.randomUUID());
        lenient().when(runs.startAttempt(any(), any())).thenReturn(UUID.randomUUID());
        lenient().when(persistence.persist(eq(PROVIDER), anyList(), anyList(), anyList(), anyMap(), anyMap(), anyMap(), anyMap()))
                .thenReturn(new RaceDataIngestionResult(PROVIDER, 960, 16));
        lenient().when(identityStore.findMapping(eq(PROVIDER_ID), any(), anyString())).thenAnswer(invocation -> {
            ReferenceEntityType type = invocation.getArgument(1);
            String externalId = invocation.getArgument(2);
            UUID id = switch (type) {
                case SESSION -> SESSION_ID;
                case RIDER -> RIDER_ID;
                case TEAM -> TEAM_ID;
                case MANUFACTURER -> MANUFACTURER_ID;
                default -> null;
            };
            return id == null ? null : new IdentityMappingStore.ExistingMapping(UUID.randomUUID(), id);
        });
    }

    @Test
    void loadsAllRaceRecordsAndRecordsSuccessfulAttempt() {
        RaceDataIngestionResult result = service.ingest(PROVIDER);

        assertThat(result.recordsSeen()).isEqualTo(960);
        verify(runs).finishAttempt(any(), eq("SUCCEEDED"), eq(960), eq(16), any());
    }

    @Test
    void rejectsLapWithoutMatchingResultAndRecordsFailure() {
        HistoricalDataProvider invalid = new HistoricalDataProvider() {
            @Override public String code() { return PROVIDER; }
            @Override public List<ExternalReference> findReferences(ReferenceQuery query) { return List.of(); }
            @Override public List<RaceData> findRaceData(RaceDataQuery query) {
                if (query.type() == RaceDataType.SESSION_RESULT) return List.of();
                if (query.type() == RaceDataType.RIDER_LAP) {
                    return List.of(new LapReference("orphan-lap", "fixture-v1", "missing-session", "missing-rider",
                            (short) 1, 1000, true, Instant.parse("2025-01-01T00:00:00Z")));
                }
                return List.of();
            }
        };
        given(providers.require(PROVIDER)).willReturn(invalid);

        assertThatThrownBy(() -> service.ingest(PROVIDER)).isInstanceOf(InvalidRaceDataException.class);
        verify(runs).finishFailedAttempt(any(), eq(1), anyString(), any());
    }

    @Test
    void rejectsDuplicateSessionResultLogicalKeyWithDifferentExternalIdsBeforePersistence() {
        HistoricalDataProvider duplicate = raceProvider(List.of(
                result("result-a", "rider-1"), result("result-b", "rider-1")), List.of(), List.of());
        given(providers.require(PROVIDER)).willReturn(duplicate);

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(InvalidRaceDataException.class)
                .hasMessageContaining("duplicate session result logical key");
        verify(persistence, never()).persist(any(), anyList(), anyList(), anyList(), anyMap(), anyMap(), anyMap(), anyMap());
    }

    @Test
    void rejectsDuplicateLapLogicalKeyWithDifferentExternalIdsBeforePersistence() {
        HistoricalDataProvider duplicate = raceProvider(List.of(result("result-a", "rider-1")), List.of(
                new LapReference("lap-a", "fixture-v1", "session-1", "rider-1", (short) 1,
                        1000, true, Instant.parse("2025-01-01T00:00:00Z")),
                new LapReference("lap-b", "fixture-v1", "session-1", "rider-1", (short) 1,
                        1001, true, Instant.parse("2025-01-01T00:00:01Z"))), List.of());
        given(providers.require(PROVIDER)).willReturn(duplicate);

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(InvalidRaceDataException.class)
                .hasMessageContaining("duplicate rider lap logical key");
        verify(persistence, never()).persist(any(), anyList(), anyList(), anyList(), anyMap(), anyMap(), anyMap(), anyMap());
    }

    @Test
    void rejectsDuplicatePositionLogicalKeyWithDifferentExternalIdsBeforePersistence() {
        HistoricalDataProvider duplicate = raceProvider(List.of(result("result-a", "rider-1")), List.of(), List.of(
                new RacePositionReference("position-a", "fixture-v1", "session-1", "rider-1", (short) 1,
                        (short) 1, Instant.parse("2025-01-01T00:00:00Z")),
                new RacePositionReference("position-b", "fixture-v1", "session-1", "rider-1", (short) 1,
                        (short) 2, Instant.parse("2025-01-01T00:00:01Z"))));
        given(providers.require(PROVIDER)).willReturn(duplicate);

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(InvalidRaceDataException.class)
                .hasMessageContaining("duplicate race position logical key");
        verify(persistence, never()).persist(any(), anyList(), anyList(), anyList(), anyMap(), anyMap(), anyMap(), anyMap());
    }

    private SessionResultReference result(String externalId, String riderExternalId) {
        return new SessionResultReference(externalId, "fixture-v1", "session-1", riderExternalId,
                null, null, (short) 1, (short) 1, "CLASSIFIED", (short) 1, 1000,
                java.math.BigDecimal.ONE);
    }

    private HistoricalDataProvider raceProvider(List<SessionResultReference> results,
                                                 List<LapReference> laps,
                                                 List<RacePositionReference> positions) {
        return new HistoricalDataProvider() {
            @Override public String code() { return PROVIDER; }
            @Override public List<ExternalReference> findReferences(ReferenceQuery query) { return List.of(); }
            @Override public List<RaceData> findRaceData(RaceDataQuery query) {
                return switch (query.type()) {
                    case SESSION_RESULT -> results.stream().map(value -> (RaceData) value).toList();
                    case RIDER_LAP -> laps.stream().map(value -> (RaceData) value).toList();
                    case RACE_POSITION -> positions.stream().map(value -> (RaceData) value).toList();
                };
            }
        };
    }
}
