package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdentityMappingServiceTest {
    private static final UUID INTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_INTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROVIDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_RECORD_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID MAPPING_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    ProviderAdapterRegistry providers;

    @Mock
    IdentityMappingStore store;

    private IdentityMappingService service;

    @BeforeEach
    void setUp() {
        service = new IdentityMappingService(providers, store, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsMappingAndPreservesSourceRecord() {
        ExternalReference reference = new ExternalReference(
                ReferenceEntityType.RIDER, "fixture-rider-001", "Fixture Rider One", "fixture-v1");
        given(providers.require(FixtureHistoricalDataProvider.CODE)).willReturn(providersAdapter(reference));
        given(store.providerId(FixtureHistoricalDataProvider.CODE)).willReturn(PROVIDER_ID);
        given(store.upsertSourceRecord(PROVIDER_ID, ReferenceEntityType.RIDER,
                "fixture-rider-001", "fixture-v1", NOW)).willReturn(SOURCE_RECORD_ID);
        given(store.insertMapping(PROVIDER_ID, ReferenceEntityType.RIDER,
                "fixture-rider-001", INTERNAL_ID, NOW)).willReturn(MAPPING_ID);

        IdentityMappingResult result = service.map(FixtureHistoricalDataProvider.CODE,
                ReferenceEntityType.RIDER, "fixture-rider-001", INTERNAL_ID);

        assertThat(result.mappingId()).isEqualTo(MAPPING_ID);
        assertThat(result.sourceRecordId()).isEqualTo(SOURCE_RECORD_ID);
        assertThat(result.outcome()).isEqualTo(IdentityMappingResult.MappingOutcome.CREATED);
        verify(store).requireInternalEntity(ReferenceEntityType.RIDER, INTERNAL_ID);
    }

    @Test
    void mapsReferenceFromFixtureProvider() {
        ProviderAdapterRegistry realProviders = new ProviderAdapterRegistry(
                List.of(new FixtureHistoricalDataProvider()));
        service = new IdentityMappingService(realProviders, store, Clock.fixed(NOW, ZoneOffset.UTC));
        given(store.providerId(FixtureHistoricalDataProvider.CODE)).willReturn(PROVIDER_ID);
        given(store.upsertSourceRecord(PROVIDER_ID, ReferenceEntityType.RIDER,
                "fixture-rider-001", "fixture-v1", NOW)).willReturn(SOURCE_RECORD_ID);
        given(store.insertMapping(PROVIDER_ID, ReferenceEntityType.RIDER,
                "fixture-rider-001", INTERNAL_ID, NOW)).willReturn(MAPPING_ID);

        IdentityMappingResult result = service.map(FixtureHistoricalDataProvider.CODE,
                ReferenceEntityType.RIDER, "fixture-rider-001", INTERNAL_ID);

        assertThat(result.outcome()).isEqualTo(IdentityMappingResult.MappingOutcome.CREATED);
        assertThat(result.sourceRecordId()).isEqualTo(SOURCE_RECORD_ID);
        verify(store).requireInternalEntity(ReferenceEntityType.RIDER, INTERNAL_ID);
    }

    @Test
    void returnsUnchangedForIdempotentMapping() {
        ExternalReference reference = new ExternalReference(
                ReferenceEntityType.RIDER, "fixture-rider-001", "Fixture Rider One", "fixture-v1");
        given(providers.require(FixtureHistoricalDataProvider.CODE)).willReturn(providersAdapter(reference));
        given(store.providerId(FixtureHistoricalDataProvider.CODE)).willReturn(PROVIDER_ID);
        given(store.upsertSourceRecord(any(), any(), any(), any(), any())).willReturn(SOURCE_RECORD_ID);
        given(store.findMapping(PROVIDER_ID, ReferenceEntityType.RIDER, "fixture-rider-001"))
                .willReturn(new IdentityMappingStore.ExistingMapping(MAPPING_ID, INTERNAL_ID));

        IdentityMappingResult result = service.map(FixtureHistoricalDataProvider.CODE,
                ReferenceEntityType.RIDER, "fixture-rider-001", INTERNAL_ID);

        assertThat(result.outcome()).isEqualTo(IdentityMappingResult.MappingOutcome.UNCHANGED);
        verify(store, never()).insertMapping(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsConflictingMappingWithoutOverwriting() {
        ExternalReference reference = new ExternalReference(
                ReferenceEntityType.RIDER, "fixture-rider-001", "Fixture Rider One", "fixture-v1");
        given(providers.require(FixtureHistoricalDataProvider.CODE)).willReturn(providersAdapter(reference));
        given(store.providerId(FixtureHistoricalDataProvider.CODE)).willReturn(PROVIDER_ID);
        given(store.upsertSourceRecord(any(), any(), any(), any(), any())).willReturn(SOURCE_RECORD_ID);
        given(store.findMapping(PROVIDER_ID, ReferenceEntityType.RIDER, "fixture-rider-001"))
                .willReturn(new IdentityMappingStore.ExistingMapping(MAPPING_ID, INTERNAL_ID));

        assertThatThrownBy(() -> service.map(FixtureHistoricalDataProvider.CODE,
                ReferenceEntityType.RIDER, "fixture-rider-001", OTHER_INTERNAL_ID))
                .isInstanceOf(IdentityMappingConflictException.class);
        verify(store, never()).insertMapping(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnknownProviderReference() {
        given(providers.require(FixtureHistoricalDataProvider.CODE)).willReturn(
                new HistoricalDataProvider() {
                    @Override public String code() { return FixtureHistoricalDataProvider.CODE; }
                    @Override public java.util.List<ExternalReference> findReferences(ReferenceQuery query) {
                        return java.util.List.of();
                    }
                });

        assertThatThrownBy(() -> service.map(FixtureHistoricalDataProvider.CODE,
                ReferenceEntityType.RIDER, "missing", INTERNAL_ID))
                .isInstanceOf(UnknownExternalReferenceException.class);
        verify(store, never()).providerId(any());
    }

    private HistoricalDataProvider providersAdapter(ExternalReference reference) {
        return new HistoricalDataProvider() {
            @Override public String code() { return FixtureHistoricalDataProvider.CODE; }
            @Override public java.util.List<ExternalReference> findReferences(ReferenceQuery query) {
                return java.util.List.of(reference);
            }
        };
    }
}
