package com.motogp.intelligence.news;

public record NewsSourceDefinition(String code, String name, String sourceType,
                                   String feedUrl, boolean enabled, int timeoutMs) {
}
