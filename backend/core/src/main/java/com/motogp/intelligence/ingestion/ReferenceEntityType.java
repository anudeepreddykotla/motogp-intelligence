package com.motogp.intelligence.ingestion;

public enum ReferenceEntityType {
    SEASON("seasons"), EVENT("events"), CIRCUIT("circuits"), RIDER("riders"),
    TEAM("teams"), MANUFACTURER("manufacturers"), SESSION("sessions"),
    TEAM_MANUFACTURER_SEASON("team_manufacturer_seasons"), RIDER_SEASON_ENTRY("rider_season_entries"),
    SESSION_RESULT("session_results"), RIDER_LAP("rider_laps"), RACE_POSITION("race_positions"),
    NEWS_ARTICLE("news_articles");

    private final String tableName;

    ReferenceEntityType(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
