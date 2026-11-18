package com.motogp.intelligence.ingestion;

import java.util.UUID;

public class IdentityMappingConflictException extends RuntimeException {
    public IdentityMappingConflictException(String providerCode, ReferenceEntityType entityType,
                                            String externalId, UUID existingInternalId, UUID requestedInternalId) {
        super("Identity mapping conflict for " + providerCode + "/" + entityType + "/" + externalId
                + ": existing internal ID " + existingInternalId + " differs from requested ID " + requestedInternalId);
    }
}
