package com.motogp.intelligence.ingestion;

public class UnknownProviderException extends RuntimeException {
    public UnknownProviderException(String providerCode) {
        super("No historical data provider is registered for code: " + providerCode);
    }
}
