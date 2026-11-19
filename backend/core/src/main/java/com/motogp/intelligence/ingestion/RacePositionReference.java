package com.motogp.intelligence.ingestion;

import java.time.Instant;

public record RacePositionReference(String externalId, String sourceVersion,
                                    String sessionExternalId, String riderExternalId,
                                    short lapNumber, Short position, Instant observedAt) implements RaceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.RACE_POSITION;
    }
}
