package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderAdapterRegistry {
    private final Map<String, HistoricalDataProvider> providers;

    public ProviderAdapterRegistry(List<HistoricalDataProvider> adapters) {
        this.providers = adapters.stream().collect(Collectors.toUnmodifiableMap(
                HistoricalDataProvider::code, Function.identity(), (first, second) -> {
                    throw new IllegalStateException("Duplicate historical provider code: " + first.code());
                }));
    }

    public HistoricalDataProvider require(String code) {
        HistoricalDataProvider provider = providers.get(code);
        if (provider == null) {
            throw new UnknownProviderException(code);
        }
        return provider;
    }
}
