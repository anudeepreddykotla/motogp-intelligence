package com.motogp.intelligence.ingestion;

import java.util.UUID;

public class InvalidInternalEntityException extends RuntimeException {
    public InvalidInternalEntityException(ReferenceEntityType entityType, UUID internalId) {
        super("Internal " + entityType + " entity does not exist: " + internalId);
    }
}
