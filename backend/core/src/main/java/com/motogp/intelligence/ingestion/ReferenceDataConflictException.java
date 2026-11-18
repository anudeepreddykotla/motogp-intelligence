package com.motogp.intelligence.ingestion;

public class ReferenceDataConflictException extends RuntimeException {
    public ReferenceDataConflictException(ReferenceEntityType entityType, String naturalKey, String message) {
        super("Reference data conflict for " + entityType + " [" + naturalKey + "]: " + message);
    }
}
