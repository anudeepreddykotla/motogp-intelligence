package com.motogp.intelligence.ingestion;

public class RaceDataConflictException extends RuntimeException {
    public RaceDataConflictException(ReferenceEntityType type, String externalId, String message) {
        super("Race data conflict for " + type + " [" + externalId + "]: " + message);
    }
}
