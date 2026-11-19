package com.motogp.intelligence.ingestion;

import java.time.Instant;

public record LapReference(String externalId, String sourceVersion, String sessionExternalId,
                           String riderExternalId, short lapNumber, Integer lapTimeMs,
                           Boolean valid, Instant observedAt) implements RaceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.RIDER_LAP;
    }
}
