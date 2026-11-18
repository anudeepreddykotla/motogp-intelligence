package com.motogp.intelligence.ingestion;

import java.util.Map;

public record ReferenceIngestionResult(String providerCode, int recordsSeen, int recordsWritten,
                                      Map<ReferenceEntityType, Integer> recordsByType) {
}
