package com.motogp.intelligence.ingestion;

public record RaceDataIngestionResult(String providerCode, int recordsSeen, int recordsWritten) {
}
