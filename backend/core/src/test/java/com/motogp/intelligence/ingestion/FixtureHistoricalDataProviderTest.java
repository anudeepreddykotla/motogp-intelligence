package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureHistoricalDataProviderTest {
    private final FixtureHistoricalDataProvider provider = new FixtureHistoricalDataProvider();

    @Test
    void returnsOnlyReferencesForRequestedEntityType() {
        var references = provider.findReferences(new ReferenceQuery(ReferenceEntityType.RIDER, null));

        assertThat(references).hasSize(2);
        assertThat(references).allMatch(reference -> reference.entityType() == ReferenceEntityType.RIDER);
    }

    @Test
    void returnsOneReferenceForKnownExternalId() {
        var reference = provider.findReference(
                new ReferenceQuery(ReferenceEntityType.RIDER, "fixture-rider-001"));

        assertThat(reference).isPresent();
        assertThat(reference.orElseThrow().displayName()).isEqualTo("Fixture Rider One");
    }
}
