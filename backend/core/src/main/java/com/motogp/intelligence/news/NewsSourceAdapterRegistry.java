package com.motogp.intelligence.news;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NewsSourceAdapterRegistry {
    private final Map<String, NewsSourceAdapter> adapters;

    public NewsSourceAdapterRegistry(List<NewsSourceAdapter> adapters) {
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(
                adapter -> adapter.sourceType().toUpperCase(), Function.identity(), (first, second) -> {
                    throw new IllegalStateException("Duplicate news source adapter: " + first.sourceType());
                }));
    }

    public NewsSourceAdapter require(String sourceType) {
        NewsSourceAdapter adapter = adapters.get(sourceType.toUpperCase());
        if (adapter == null && (sourceType.equalsIgnoreCase("RSS") || sourceType.equalsIgnoreCase("ATOM"))) {
            adapter = adapters.get("RSS_ATOM");
        }
        if (adapter == null) throw new NewsSourceAdapterException("Unsupported news source type: " + sourceType);
        return adapter;
    }
}
