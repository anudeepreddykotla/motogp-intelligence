package com.motogp.intelligence.ingestion;

import java.time.Instant;

public record SessionReference(String externalId, String sourceVersion, String eventExternalId,
                               String sessionType, Instant scheduledStartAt, Instant startedAt,
                               Instant endedAt, String dataMode) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.SESSION;
    }

    @Override
    public String displayName() {
        return sessionType;
    }
}
