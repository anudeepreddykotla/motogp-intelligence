package com.motogp.intelligence.news;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class NewsArticleStore {
    private final NamedParameterJdbcTemplate jdbc;

    public NewsArticleStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean upsert(UUID sourceId, UUID sourceRecordId, NewsArticleRecord article, Instant fetchedAt) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM news_articles WHERE deduplication_key = :deduplicationKey)
                """, new MapSqlParameterSource().addValue("deduplicationKey", article.deduplicationKey()), Boolean.class);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sourceId", sourceId).addValue("sourceRecordId", sourceRecordId)
                .addValue("canonicalUrl", article.canonicalUrl()).addValue("deduplicationKey", article.deduplicationKey())
                .addValue("originalUrl", article.originalUrl()).addValue("title", article.title())
                .addValue("publishedAt", timestamp(article.publishedAt())).addValue("excerpt", article.permittedExcerpt())
                .addValue("imageUrl", article.imageUrl()).addValue("contentHash", article.contentHash())
                .addValue("fetchedAt", timestamp(fetchedAt));
        jdbc.update("""
                INSERT INTO news_articles (source_id, canonical_url, deduplication_key, original_url, title,
                    published_at, permitted_excerpt, image_url, content_hash, fetched_at, primary_source_record_id)
                VALUES (:sourceId, :canonicalUrl, :deduplicationKey, :originalUrl, :title,
                    :publishedAt, :excerpt, :imageUrl, :contentHash, :fetchedAt, :sourceRecordId)
                ON CONFLICT (deduplication_key) DO UPDATE SET
                    original_url = EXCLUDED.original_url, title = EXCLUDED.title,
                    published_at = EXCLUDED.published_at, permitted_excerpt = EXCLUDED.permitted_excerpt,
                    image_url = EXCLUDED.image_url, content_hash = EXCLUDED.content_hash,
                    fetched_at = EXCLUDED.fetched_at, updated_at = now()
                """, params);
        return !Boolean.TRUE.equals(exists);
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
