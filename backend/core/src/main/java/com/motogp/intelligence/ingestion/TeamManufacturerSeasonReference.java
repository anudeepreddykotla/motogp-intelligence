package com.motogp.intelligence.ingestion;

public record TeamManufacturerSeasonReference(
        String externalId, String sourceVersion, String seasonExternalId,
        String teamExternalId, String manufacturerExternalId,
        String validFromEventExternalId, String validToEventExternalId)
        implements ReferenceRelationshipData {
    @Override
    public ReferenceRelationshipType relationshipType() {
        return ReferenceRelationshipType.TEAM_MANUFACTURER_SEASON;
    }
}
