package com.motogp.intelligence.ingestion;

public class InvalidReferenceDataException extends RuntimeException {
    public InvalidReferenceDataException(String providerCode, ReferenceEntityType entityType,
                                         String externalId, String message) {
        super("Invalid " + entityType + " reference from " + providerCode + "/" + externalId + ": " + message);
    }
}
