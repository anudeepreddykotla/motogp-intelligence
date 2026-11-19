package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureHistoricalDataProviderTest {
    private final FixtureHistoricalDataProvider provider = new FixtureHistoricalDataProvider();

    @Test
    void returnsOnlyReferencesForRequestedEntityType() {
        var references = provider.findReferences(new ReferenceQuery(ReferenceEntityType.RIDER, null));

        assertThat(references).hasSize(10);
        assertThat(references).allMatch(reference -> reference.entityType() == ReferenceEntityType.RIDER);
    }

    @Test
    void returnsOneReferenceForKnownExternalId() {
        var reference = provider.findReference(
                new ReferenceQuery(ReferenceEntityType.RIDER, "fixture-rider-001"));

        assertThat(reference).isPresent();
        assertThat(reference.orElseThrow().displayName()).isEqualTo("Fixture Rider One");
    }

    @Test
    void exposesExpectedSyntheticDatasetShape() {
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.SEASON, null))).hasSize(2);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.MANUFACTURER, null))).hasSize(2);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.TEAM, null))).hasSize(4);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.RIDER, null))).hasSize(10);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.CIRCUIT, null))).hasSize(2);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.EVENT, null))).hasSize(4);
        assertThat(provider.findReferenceData(new ReferenceQuery(ReferenceEntityType.SESSION, null))).hasSize(12);
        assertThat(provider.findReferenceRelationships()).hasSize(28);
        assertThat(provider.findRaceData(new RaceDataQuery(RaceDataType.SESSION_RESULT, null))).hasSize(120);
        assertThat(provider.findRaceData(new RaceDataQuery(RaceDataType.RIDER_LAP, null))).hasSize(400);
        assertThat(provider.findRaceData(new RaceDataQuery(RaceDataType.RACE_POSITION, null))).hasSize(440);
    }
}
