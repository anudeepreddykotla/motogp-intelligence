package com.motogp.intelligence.ingestion;

import java.math.BigDecimal;

public record CircuitReference(String externalId, String sourceVersion,
                               String name, String countryCode, String timezone,
                               BigDecimal latitude, BigDecimal longitude) implements ReferenceData {
    @Override
    public ReferenceEntityType entityType() {
        return ReferenceEntityType.CIRCUIT;
    }

    @Override
    public String displayName() {
        return name;
    }
}
