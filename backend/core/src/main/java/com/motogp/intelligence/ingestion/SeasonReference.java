package com.motogp.intelligence.ingestion;

public record SeasonReference(String externalId, String sourceVersion, short year,
                              String championship) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.SEASON;
    }

    @Override
    public String displayName() {
        return championship + " " + year;
    }
}
