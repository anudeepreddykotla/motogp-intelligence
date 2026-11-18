package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class FixtureHistoricalDataProvider implements HistoricalDataProvider {
    public static final String CODE = "fixture-historical";

    private static final List<ReferenceData> REFERENCE_DATA = List.of(
            new SeasonReference("fixture-season-2025", "fixture-v1", (short) 2025, "MotoGP"),
            new ManufacturerReference("fixture-manufacturer-001", "fixture-v1", "Fixture Works"),
            new TeamReference("fixture-team-001", "fixture-v1", "Fixture Racing", "IT"),
            new RiderReference("fixture-rider-001", "fixture-v1", "Fixture Rider One", "Fixture", "Rider One", "IT"),
            new RiderReference("fixture-rider-002", "fixture-v1", "Fixture Rider Two", "Fixture", "Rider Two", "ES"),
            new CircuitReference("fixture-circuit-mugello", "fixture-v1", "Mugello", "IT", "Europe/Rome",
                    new BigDecimal("43.9975"), new BigDecimal("11.3719")),
            new EventReference("fixture-event-mugello-2025", "fixture-v1", "Italian Grand Prix",
                    "fixture-season-2025", "fixture-circuit-mugello", (short) 6,
                    Instant.parse("2025-06-20T08:00:00Z"), "Europe/Rome"));

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ExternalReference> findReferences(ReferenceQuery query) {
        return findReferenceData(query).stream()
                .map(ReferenceData::asExternalReference)
                .toList();
    }

    @Override
    public List<ReferenceData> findReferenceData(ReferenceQuery query) {
        return REFERENCE_DATA.stream()
                .filter(reference -> reference.entityType() == query.entityType())
                .filter(reference -> query.externalId() == null || reference.externalId().equals(query.externalId()))
                .toList();
    }
}
