package com.motogp.intelligence.ingestion;

import java.math.BigDecimal;

public record SessionResultReference(String externalId, String sourceVersion,
                                    String sessionExternalId, String riderExternalId,
                                    String teamExternalId, String manufacturerExternalId,
                                    Short gridPosition, Short classificationPosition,
                                    String classificationStatus, Short lapsCompleted,
                                    Integer totalTimeMs, BigDecimal points) implements RaceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.SESSION_RESULT;
    }
}
