package com.motogp.intelligence.ingestion;

public sealed interface RaceData
        permits SessionResultReference, LapReference, RacePositionReference {
    ReferenceEntityType entityType();

    String externalId();

    String sourceVersion();
}
