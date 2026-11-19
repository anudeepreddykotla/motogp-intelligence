package com.motogp.intelligence.news;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}",
        "motogp.news.sources[0].code=fixture-rss",
        "motogp.news.sources[0].name=Fixture RSS",
        "motogp.news.sources[0].source-type=RSS",
        "motogp.news.sources[0].feed-url=classpath:/news/fixture-rss.xml",
        "motogp.news.sources[1].code=fixture-invalid",
        "motogp.news.sources[1].name=Fixture Invalid",
        "motogp.news.sources[1].source-type=RSS_ATOM",
        "motogp.news.sources[1].feed-url=classpath:/news/invalid.xml"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class NewsIngestionComposeIntegrationTest {
    @Autowired
    private NewsIngestionService ingestion;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void ingestsFixtureMetadataIdempotentlyAndTracksSourceHealth() {
        var first = ingestion.ingestConfiguredSources();
        var second = ingestion.ingestConfiguredSources();

        assertThat(first.status()).isEqualTo("PARTIAL");
        assertThat(first.recordsSeen()).isEqualTo(3);
        assertThat(first.recordsWritten()).isEqualTo(2);
        assertThat(first.successfulSources()).isEqualTo(1);
        assertThat(first.failedSources()).isEqualTo(1);
        assertThat(second.recordsWritten()).isZero();
        assertThat(count("news_articles")).isEqualTo(2);
        assertThat(count("news_sources")).isEqualTo(2);
        assertThat(count("source_records")).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM news_sources
                WHERE code = 'fixture-rss' AND last_successful_fetch_at IS NOT NULL
                  AND last_failed_at IS NULL AND last_error IS NULL
                """, Map.of(), Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM news_sources
                WHERE code = 'fixture-invalid' AND last_failed_at IS NOT NULL
                  AND last_error IS NOT NULL
                """, Map.of(), Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM ingestion_attempts ia
                JOIN ingestion_jobs ij ON ij.id = ia.job_id
                JOIN providers p ON p.id = ij.provider_id
                WHERE p.code = 'news-rss' AND ij.job_type = 'NEWS' AND ia.status = 'PARTIAL'
                """, Map.of(), Long.class)).isEqualTo(2);
    }

    private long count(String table) {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
