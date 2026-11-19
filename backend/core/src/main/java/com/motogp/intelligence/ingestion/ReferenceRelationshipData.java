package com.motogp.intelligence.ingestion;

public sealed interface ReferenceRelationshipData
        permits TeamManufacturerSeasonReference, RiderSeasonEntryReference {
    ReferenceRelationshipType relationshipType();
    String externalId();
    String sourceVersion();
}
