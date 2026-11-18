package com.motogp.intelligence.ingestion;

public class UnknownExternalReferenceException extends RuntimeException {
    public UnknownExternalReferenceException(String providerCode, ReferenceEntityType entityType, String externalId) {
        super("Provider reference was not found: " + providerCode + "/" + entityType + "/" + externalId);
    }
}
