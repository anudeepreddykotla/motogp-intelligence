package com.motogp.intelligence.news;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NewsReadStore {
    private final NamedParameterJdbcTemplate jdbc;

    public NewsReadStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<NewsArticleView> articles(String title, String publisher, Instant publishedFrom,
                                          Instant publishedTo, int limit, int offset) {
        return jdbc.query("""
                SELECT article.id, article.title, article.original_url, article.published_at,
                       article.permitted_excerpt, article.image_url, source.name AS publisher_name
                FROM news_articles article
                JOIN news_sources source ON source.id = article.source_id
                WHERE (CAST(:title AS text) IS NULL OR lower(article.title) LIKE lower('%' || :title || '%'))
                  AND (CAST(:publisher AS text) IS NULL OR lower(source.name) = lower(:publisher))
                  AND (CAST(:publishedFrom AS timestamptz) IS NULL OR article.published_at >= :publishedFrom)
                  AND (CAST(:publishedTo AS timestamptz) IS NULL OR article.published_at <= :publishedTo)
                ORDER BY article.published_at DESC NULLS LAST, article.id DESC
                LIMIT :limit OFFSET :offset
                """, params().addValue("title", title).addValue("publisher", publisher)
                        .addValue("publishedFrom", timestamp(publishedFrom)).addValue("publishedTo", timestamp(publishedTo))
                        .addValue("limit", limit + 1).addValue("offset", offset), (rs, row) -> articleView(rs, row));
    }

    public Optional<NewsArticleView> article(UUID articleId) {
        return jdbc.query("""
                SELECT article.id, article.title, article.original_url, article.published_at,
                       article.permitted_excerpt, article.image_url, source.name AS publisher_name
                FROM news_articles article
                JOIN news_sources source ON source.id = article.source_id
                WHERE article.id = :articleId
                """, params().addValue("articleId", articleId),
                rows -> rows.next() ? Optional.of(articleView(rows, 0)) : Optional.empty());
    }

    public List<String> publishers() {
        return jdbc.queryForList("""
                SELECT DISTINCT source.name FROM news_sources source
                JOIN news_articles article ON article.source_id = source.id
                ORDER BY source.name
                """, params(), String.class);
    }

    private NewsArticleView articleView(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new NewsArticleView(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("publisher_name"), instant(rs.getTimestamp("published_at")),
                rs.getString("permitted_excerpt"), rs.getString("image_url"), rs.getString("original_url"));
    }

    private MapSqlParameterSource params() { return new MapSqlParameterSource(); }
    private Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }

    public record NewsArticleView(UUID id, String title, String publisherName, Instant publishedAt,
                                  String permittedExcerpt, String imageUrl, String originalUrl) { }
}
