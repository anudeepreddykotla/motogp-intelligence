package com.motogp.intelligence.ingestion;

public record RiderReference(String externalId, String sourceVersion,
                             String displayName, String givenName, String familyName,
                             String nationalityCode) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.RIDER;
    }
}
