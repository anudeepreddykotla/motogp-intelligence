package com.motogp.intelligence.ingestion;

public enum ReferenceEntityType {
    SEASON("seasons"), EVENT("events"), CIRCUIT("circuits"), RIDER("riders"),
    TEAM("teams"), MANUFACTURER("manufacturers"), SESSION("sessions");

    private final String tableName;

    ReferenceEntityType(String tableName) {
        this.tableName = tableName;
    }

    public String tableName() {
        return tableName;
    }
}
