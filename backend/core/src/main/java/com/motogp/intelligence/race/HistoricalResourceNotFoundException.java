package com.motogp.intelligence.race;

public class HistoricalResourceNotFoundException extends RuntimeException {
    public HistoricalResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
