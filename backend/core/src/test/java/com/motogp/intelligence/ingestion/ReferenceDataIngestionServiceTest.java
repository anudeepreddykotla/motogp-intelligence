package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReferenceDataIngestionServiceTest {
    private static final String PROVIDER = FixtureHistoricalDataProvider.CODE;
    private static final UUID SEASON_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID MANUFACTURER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID RIDER_ONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
    private static final UUID RIDER_TWO_ID = UUID.fromString("00000000-0000-0000-0000-000000000105");
    private static final UUID CIRCUIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000106");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000107");

    @Mock
    ReferenceDataStore store;

    @Mock
    IdentityMappingService identityMappings;

    @Mock
    IngestionRunStore ingestionRuns;

    private ReferenceDataIngestionService service;

    @BeforeEach
    void setUp() {
        HistoricalDataProvider fixture = new FixtureHistoricalDataProvider();
        service = new ReferenceDataIngestionService(
                new ProviderAdapterRegistry(List.of(fixture)), identityMappings, store, ingestionRuns);
    }

    @Test
    void ingestsAllFixtureReferencesInDependencyOrder() {
        stubNewRows();
        given(identityMappings.map(eq(PROVIDER), any(), anyString(), any())).willAnswer(invocation ->
                new IdentityMappingResult(UUID.randomUUID(), UUID.randomUUID(), PROVIDER,
                        invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3),
                        IdentityMappingResult.MappingOutcome.CREATED));

        ReferenceIngestionResult result = service.ingest(PROVIDER);

        assertThat(result.recordsSeen()).isEqualTo(7);
        assertThat(result.recordsWritten()).isEqualTo(7);
        assertThat(result.recordsByType()).containsEntry(ReferenceEntityType.SEASON, 1)
                .containsEntry(ReferenceEntityType.EVENT, 1);
        verify(ingestionRuns).ensureReferenceJob(PROVIDER);
        verify(ingestionRuns).startAttempt(any(), any());
        verify(ingestionRuns).finishAttempt(any(), eq("SUCCEEDED"), eq(7), eq(7), any());
        InOrder order = inOrder(identityMappings);
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.SEASON),
                eq("fixture-season-2025"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.MANUFACTURER),
                eq("fixture-manufacturer-001"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.CIRCUIT),
                eq("fixture-circuit-mugello"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.TEAM),
                eq("fixture-team-001"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.RIDER),
                eq("fixture-rider-001"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.RIDER),
                eq("fixture-rider-002"), any());
        order.verify(identityMappings).map(eq(PROVIDER), eq(ReferenceEntityType.EVENT),
                eq("fixture-event-mugello-2025"), any());
    }

    @Test
    void rerunningExistingFixtureIsIdempotent() {
        stubExistingRows();
        given(identityMappings.map(eq(PROVIDER), any(), anyString(), any())).willAnswer(invocation ->
                new IdentityMappingResult(UUID.randomUUID(), UUID.randomUUID(), PROVIDER,
                        invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3),
                        IdentityMappingResult.MappingOutcome.UNCHANGED));

        ReferenceIngestionResult result = service.ingest(PROVIDER);

        assertThat(result.recordsSeen()).isEqualTo(7);
        assertThat(result.recordsWritten()).isZero();
        verify(store, never()).insertSeason(any());
        verify(store, never()).insertManufacturer(any());
        verify(store, never()).insertTeam(any());
        verify(store, never()).insertRider(any());
        verify(store, never()).insertCircuit(any());
        verify(store, never()).insertEvent(any(), any(), any());
    }

    @Test
    void rejectsNaturalKeyConflictWithoutContinuing() {
        given(store.findSeason(anyShort(), anyString())).willReturn(Optional.empty());
        given(store.findManufacturer(anyString())).willReturn(Optional.empty());
        given(store.findCircuit(anyString(), any())).willReturn(Optional.empty());
        given(store.findTeam(anyString(), any())).willReturn(Optional.empty());
        given(store.insertSeason(any())).willReturn(SEASON_ID);
        given(store.insertManufacturer(any())).willReturn(MANUFACTURER_ID);
        given(store.insertCircuit(any())).willReturn(CIRCUIT_ID);
        given(store.insertTeam(any())).willReturn(TEAM_ID);
        given(identityMappings.map(eq(PROVIDER), any(), anyString(), any())).willAnswer(invocation ->
                new IdentityMappingResult(UUID.randomUUID(), UUID.randomUUID(), PROVIDER,
                        invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3),
                        IdentityMappingResult.MappingOutcome.CREATED));
        given(store.findRider("Fixture Rider One")).willReturn(Optional.of(new ReferenceDataStore.RiderRow(
                RIDER_ONE_ID, "Fixture Rider One", "Different", "Rider One", "IT")));

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(ReferenceDataConflictException.class);
        verify(store, never()).insertEvent(any(), any(), any());
    }

    @Test
    void rejectsDuplicateExternalIdentifiers() {
        HistoricalDataProvider duplicateProvider = providerOf(List.of(
                new ManufacturerReference("duplicate", "fixture-v1", "First"),
                new ManufacturerReference("duplicate", "fixture-v1", "Second")));
        service = new ReferenceDataIngestionService(new ProviderAdapterRegistry(List.of(duplicateProvider)),
                identityMappings, store, ingestionRuns);

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(DuplicateReferenceException.class);
        verify(identityMappings, never()).map(any(), any(), anyString(), any());
    }

    @Test
    void rejectsEventWithMissingDependency() {
        HistoricalDataProvider invalidProvider = providerOf(List.of(new EventReference(
                "event-without-season", "fixture-v1", "Invalid Event", "missing-season",
                "missing-circuit", (short) 1, Instant.parse("2025-01-01T00:00:00Z"), "UTC")));
        service = new ReferenceDataIngestionService(new ProviderAdapterRegistry(List.of(invalidProvider)),
                identityMappings, store, ingestionRuns);

        assertThatThrownBy(() -> service.ingest(PROVIDER))
                .isInstanceOf(InvalidReferenceDataException.class);
        verify(store, never()).insertEvent(any(), any(), any());
    }

    private void stubNewRows() {
        given(store.findSeason(anyShort(), anyString())).willReturn(Optional.empty());
        given(store.findManufacturer(anyString())).willReturn(Optional.empty());
        given(store.findTeam(anyString(), any())).willReturn(Optional.empty());
        given(store.findRider(anyString())).willReturn(Optional.empty());
        given(store.findCircuit(anyString(), any())).willReturn(Optional.empty());
        given(store.findEvent(any(), anyString())).willReturn(Optional.empty());
        given(store.insertSeason(any())).willReturn(SEASON_ID);
        given(store.insertManufacturer(any())).willReturn(MANUFACTURER_ID);
        given(store.insertTeam(any())).willReturn(TEAM_ID);
        given(store.insertRider(any(RiderReference.class))).willReturn(RIDER_ONE_ID, RIDER_TWO_ID);
        given(store.insertCircuit(any())).willReturn(CIRCUIT_ID);
        given(store.insertEvent(any(), any(), any())).willReturn(EVENT_ID);
    }

    private void stubExistingRows() {
        given(store.findSeason((short) 2025, "MotoGP")).willReturn(Optional.of(
                new ReferenceDataStore.SeasonRow(SEASON_ID, (short) 2025, "MotoGP")));
        given(store.findManufacturer("Fixture Works")).willReturn(Optional.of(
                new ReferenceDataStore.ManufacturerRow(MANUFACTURER_ID, "Fixture Works")));
        given(store.findTeam("Fixture Racing", "IT")).willReturn(Optional.of(
                new ReferenceDataStore.TeamRow(TEAM_ID, "Fixture Racing", "IT")));
        given(store.findRider("Fixture Rider One")).willReturn(Optional.of(
                new ReferenceDataStore.RiderRow(RIDER_ONE_ID, "Fixture Rider One", "Fixture", "Rider One", "IT")));
        given(store.findRider("Fixture Rider Two")).willReturn(Optional.of(
                new ReferenceDataStore.RiderRow(RIDER_TWO_ID, "Fixture Rider Two", "Fixture", "Rider Two", "ES")));
        given(store.findCircuit("Mugello", "IT")).willReturn(Optional.of(
                new ReferenceDataStore.CircuitRow(CIRCUIT_ID, "Mugello", "IT", "Europe/Rome",
                        new BigDecimal("43.9975"), new BigDecimal("11.3719"))));
        given(store.findEvent(SEASON_ID, "Italian Grand Prix")).willReturn(Optional.of(
                new ReferenceDataStore.EventRow(EVENT_ID, SEASON_ID, CIRCUIT_ID, "Italian Grand Prix", (short) 6,
                        Instant.parse("2025-06-20T08:00:00Z"), "Europe/Rome")));
    }

    private HistoricalDataProvider providerOf(List<ReferenceData> records) {
        return new HistoricalDataProvider() {
            @Override
            public String code() {
                return PROVIDER;
            }

            @Override
            public List<ExternalReference> findReferences(ReferenceQuery query) {
                return records.stream().map(ReferenceData::asExternalReference).toList();
            }

            @Override
            public List<ReferenceData> findReferenceData(ReferenceQuery query) {
                return records.stream().filter(record -> record.entityType() == query.entityType()).toList();
            }
        };
    }
}
