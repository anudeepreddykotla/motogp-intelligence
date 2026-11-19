package com.motogp.intelligence.news;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RssAtomNewsSourceAdapterTest {
    private final RssAtomNewsSourceAdapter adapter = new RssAtomNewsSourceAdapter();

    @Test
    void normalizesRssMetadataAndFallbackFingerprint() {
        var records = adapter.fetch(new NewsSourceDefinition("fixture", "Fixture", "RSS",
                "classpath:/news/fixture-rss.xml", true, 1000));

        assertThat(records).hasSize(3);
        assertThat(records.get(0).canonicalUrl()).isEqualTo("https://news.example.test/articles/fixture-rider");
        assertThat(records.get(0).publishedAt()).isNotNull();
        assertThat(records.get(0).imageUrl()).isEqualTo("https://news.example.test/images/fixture.jpg");
        assertThat(records.get(1).originalUrl()).isNull();
        assertThat(records.get(1).canonicalUrl()).startsWith("urn:news:");
        assertThat(records.get(0).contentHash()).isNotBlank();
    }

    @Test
    void readsAtomLinksAndDates() {
        var records = adapter.fetch(new NewsSourceDefinition("fixture", "Fixture", "ATOM",
                "classpath:/news/fixture-atom.xml", true, 1000));

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.originalUrl()).isEqualTo("https://news.example.test/atom/one");
            assertThat(record.permittedExcerpt()).isEqualTo("Atom excerpt.");
            assertThat(record.publishedAt()).isNotNull();
        });
    }

    @Test
    void rejectsMalformedXml() {
        assertThatThrownBy(() -> adapter.fetch(new NewsSourceDefinition("fixture", "Fixture", "RSS_ATOM",
                "classpath:/news/invalid.xml", true, 1000)))
                .isInstanceOf(NewsSourceAdapterException.class);
    }
}
