package com.motogp.intelligence.ingestion;

public class DuplicateReferenceException extends RuntimeException {
    public DuplicateReferenceException(String providerCode, ReferenceEntityType entityType, String externalId) {
        super("Provider returned duplicate reference: " + providerCode + "/" + entityType + "/" + externalId);
    }
}
