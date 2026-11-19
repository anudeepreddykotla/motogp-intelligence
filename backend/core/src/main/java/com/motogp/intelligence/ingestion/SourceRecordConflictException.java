package com.motogp.intelligence.ingestion;

import java.util.UUID;

public class SourceRecordConflictException extends RuntimeException {
    public SourceRecordConflictException(UUID providerId, ReferenceEntityType entityType,
                                         String externalId, String existingVersion, String requestedVersion) {
        super("Source record conflict for provider " + providerId + "/" + entityType + "/" + externalId
                + ": existing source version " + existingVersion
                + " differs from requested source version " + requestedVersion);
    }
}
