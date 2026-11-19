package com.motogp.intelligence.ingestion;

public class InvalidRaceDataException extends RuntimeException {
    public InvalidRaceDataException(String providerCode, ReferenceEntityType type,
                                    String externalId, String message) {
        super("Invalid " + type + " race data from " + providerCode + "/" + externalId + ": " + message);
    }
}
