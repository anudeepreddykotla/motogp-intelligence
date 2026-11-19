package com.motogp.intelligence.ingestion;

public record RiderSeasonEntryReference(
        String externalId, String sourceVersion, String seasonExternalId,
        String riderExternalId, String teamExternalId, String manufacturerExternalId,
        String competitionClass, Short racingNumber,
        String validFromEventExternalId, String validToEventExternalId)
        implements ReferenceRelationshipData {
    @Override
    public ReferenceRelationshipType relationshipType() {
        return ReferenceRelationshipType.RIDER_SEASON_ENTRY;
    }
}
