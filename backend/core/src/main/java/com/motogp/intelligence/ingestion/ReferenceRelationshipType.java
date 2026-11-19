package com.motogp.intelligence.ingestion;

public enum ReferenceRelationshipType {
    TEAM_MANUFACTURER_SEASON("team_manufacturer_seasons"),
    RIDER_SEASON_ENTRY("rider_season_entries");

    private final String tableName;

    ReferenceRelationshipType(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
