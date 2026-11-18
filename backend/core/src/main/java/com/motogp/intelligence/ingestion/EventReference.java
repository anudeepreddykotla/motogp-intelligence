package com.motogp.intelligence.ingestion;

import java.time.Instant;

public record EventReference(String externalId, String sourceVersion,
                             String name, String seasonExternalId, String circuitExternalId,
                             short roundNumber, Instant scheduledStartAt,
                             String timezone) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.EVENT;
    }

    @Override
    public String displayName() {
        return name;
    }
}
