package com.motogp.intelligence.ingestion;

import java.util.List;
import java.util.Optional;

/** Provider-specific transport and parsing stop at this normalized adapter boundary. */
public interface HistoricalDataProvider {
    String code();

    List<ExternalReference> findReferences(ReferenceQuery query);

    /** Returns normalized reference records; provider-specific parsing must remain inside the adapter. */
    default List<ReferenceData> findReferenceData(ReferenceQuery query) {
        throw new UnsupportedOperationException("Provider does not expose normalized reference data: " + code());
    }

    default Optional<ExternalReference> findReference(ReferenceQuery query) {
        return findReferences(query).stream().findFirst();
    }
}
