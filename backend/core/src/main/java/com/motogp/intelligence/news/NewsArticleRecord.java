package com.motogp.intelligence.news;

import java.time.Instant;

public record NewsArticleRecord(String title, String originalUrl, String canonicalUrl,
                                Instant publishedAt, String permittedExcerpt, String imageUrl,
                                String contentHash, String deduplicationKey) {
}
