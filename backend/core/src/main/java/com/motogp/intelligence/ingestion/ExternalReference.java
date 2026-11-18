package com.motogp.intelligence.ingestion;

public record ExternalReference(ReferenceEntityType entityType, String externalId,
                                String displayName, String sourceVersion) {
}
