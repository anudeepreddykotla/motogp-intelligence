package com.motogp.intelligence.news;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}",
        "motogp.news.sources[0].code=fixture-rss",
        "motogp.news.sources[0].name=Fixture RSS",
        "motogp.news.sources[0].source-type=RSS",
        "motogp.news.sources[0].feed-url=classpath:/news/fixture-rss.xml"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class NewsReadStoreComposeIntegrationTest {
    @Autowired
    private NewsIngestionService ingestion;

    @Autowired
    private NewsReadStore store;

    @Test
    void readsFilteredStablePaginatedNewsMetadata() {
        ingestion.ingestConfiguredSources();

        var firstPage = store.articles(null, null, null, null, 1, 0);
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).publishedAt()).isAfterOrEqualTo(firstPage.get(1).publishedAt());

        var filtered = store.articles("fixture", "Fixture RSS",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T23:59:59Z"), 10, 0);
        assertThat(filtered).isNotEmpty();
        assertThat(store.publishers()).containsExactly("Fixture RSS");
        assertThat(store.article(filtered.get(0).id())).contains(filtered.get(0));
    }
}
