package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixtureHistoricalDataProvider implements HistoricalDataProvider {
    public static final String CODE = "fixture-historical";

    private static final List<ExternalReference> REFERENCES = List.of(
            new ExternalReference(ReferenceEntityType.SEASON, "fixture-season-2025", "MotoGP 2025", "fixture-v1"),
            new ExternalReference(ReferenceEntityType.CIRCUIT, "fixture-circuit-mugello", "Mugello", "fixture-v1"),
            new ExternalReference(ReferenceEntityType.RIDER, "fixture-rider-001", "Fixture Rider One", "fixture-v1"),
            new ExternalReference(ReferenceEntityType.RIDER, "fixture-rider-002", "Fixture Rider Two", "fixture-v1"),
            new ExternalReference(ReferenceEntityType.TEAM, "fixture-team-001", "Fixture Racing", "fixture-v1"),
            new ExternalReference(ReferenceEntityType.MANUFACTURER, "fixture-manufacturer-001", "Fixture Works", "fixture-v1"));

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ExternalReference> findReferences(ReferenceQuery query) {
        return REFERENCES.stream()
                .filter(reference -> reference.entityType() == query.entityType())
                .filter(reference -> query.externalId() == null || reference.externalId().equals(query.externalId()))
                .toList();
    }
}
