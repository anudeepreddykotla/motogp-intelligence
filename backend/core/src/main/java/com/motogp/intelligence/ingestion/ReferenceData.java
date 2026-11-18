package com.motogp.intelligence.ingestion;

/** Normalized provider data consumed by the reference-data persistence boundary. */
public sealed interface ReferenceData
        permits SeasonReference, ManufacturerReference, TeamReference, RiderReference,
        CircuitReference, EventReference {
    ReferenceEntityType entityType();

    String externalId();

    String sourceVersion();

    String displayName();

    default ExternalReference asExternalReference() {
        return new ExternalReference(entityType(), externalId(), displayName(), sourceVersion());
    }
}
