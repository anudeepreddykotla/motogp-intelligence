package com.motogp.intelligence.ingestion;

import java.util.UUID;

public record IdentityMappingResult(UUID mappingId, UUID sourceRecordId, String providerCode,
                                    ReferenceEntityType entityType, String externalId, UUID internalId,
                                    MappingOutcome outcome) {
    public enum MappingOutcome { CREATED, UNCHANGED }
}
