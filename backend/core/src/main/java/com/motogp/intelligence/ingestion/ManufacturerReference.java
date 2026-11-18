package com.motogp.intelligence.ingestion;

public record ManufacturerReference(String externalId, String sourceVersion,
                                    String name) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.MANUFACTURER;
    }

    @Override
    public String displayName() {
        return name;
    }
}
