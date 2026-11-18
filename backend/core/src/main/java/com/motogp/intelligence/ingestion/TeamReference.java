package com.motogp.intelligence.ingestion;

public record TeamReference(String externalId, String sourceVersion,
                            String name, String countryCode) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.TEAM;
    }

    @Override
    public String displayName() {
        return name;
    }
}
