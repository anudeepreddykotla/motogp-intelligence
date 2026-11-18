package com.motogp.intelligence.ingestion;

import java.util.List;
import java.util.Optional;

/** Provider-specific transport and parsing stop at this normalized adapter boundary. */
public interface HistoricalDataProvider {
    String code();

    List<ExternalReference> findReferences(ReferenceQuery query);

    default Optional<ExternalReference> findReference(ReferenceQuery query) {
        return findReferences(query).stream().findFirst();
    }
}
