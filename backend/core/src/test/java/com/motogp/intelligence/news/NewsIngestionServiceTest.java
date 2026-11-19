package com.motogp.intelligence.news;

import com.motogp.intelligence.ingestion.IdentityMappingStore;
import com.motogp.intelligence.ingestion.IngestionRunStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NewsIngestionServiceTest {
    @Test
    void deduplicatesEntriesAndRecordsSuccessfulAttempt() {
        var properties = new NewsProperties();
        var source = new NewsProperties.Source();
        source.setCode("fixture"); source.setName("Fixture"); source.setSourceType("RSS_ATOM");
        source.setFeedUrl("fixture"); properties.setSources(List.of(source));
        var adapter = mock(NewsSourceAdapter.class);
        when(adapter.sourceType()).thenReturn("RSS_ATOM");
        var registry = new NewsSourceAdapterRegistry(List.of(adapter));
        var article = new NewsArticleRecord("Title", "https://example.test/1", "https://example.test/1",
                null, "Excerpt", null, "hash", "url:https://example.test/1");
        when(adapter.fetch(any())).thenReturn(List.of(article, article));
        var sources = mock(NewsSourceStore.class);
        when(sources.upsert(any(), any())).thenReturn(java.util.UUID.randomUUID());
        var persistence = mock(NewsArticlePersistenceService.class);
        when(persistence.persist(any(), any(), any(), any())).thenReturn(1);
        var identity = mock(IdentityMappingStore.class);
        when(identity.providerId(NewsIngestionService.PROVIDER_CODE)).thenReturn(java.util.UUID.randomUUID());
        var runs = mock(IngestionRunStore.class);
        when(runs.ensureNewsJob(any())).thenReturn(java.util.UUID.randomUUID());
        when(runs.startAttempt(any(), any())).thenReturn(java.util.UUID.randomUUID());

        var service = new NewsIngestionService(properties, registry, sources, persistence, identity, runs,
                java.time.Clock.systemUTC());
        var result = service.ingestConfiguredSources();

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.recordsSeen()).isEqualTo(2);
        assertThat(result.recordsWritten()).isEqualTo(1);
        verify(persistence).persist(any(), any(), eq(List.of(article)), any());
        verify(runs).finishAttempt(any(), eq("SUCCEEDED"), eq(2), eq(1), any());
    }

    @Test
    void recordsFailedSourceWithoutAbortingOtherSources() {
        var properties = new NewsProperties();
        properties.setSources(List.of(source("bad", "bad"), source("good", "good")));
        var bad = mock(NewsSourceAdapter.class); when(bad.sourceType()).thenReturn("RSS_ATOM");
        when(bad.fetch(any())).thenThrow(new NewsSourceAdapterException("timeout"));
        var good = mock(NewsSourceAdapter.class); when(good.sourceType()).thenReturn("GOOD");
        var registry = new NewsSourceAdapterRegistry(List.of(bad, good));
        var sources = mock(NewsSourceStore.class); when(sources.upsert(any(), any())).thenReturn(java.util.UUID.randomUUID());
        var persistence = mock(NewsArticlePersistenceService.class);
        var identity = mock(IdentityMappingStore.class); when(identity.providerId(any())).thenReturn(java.util.UUID.randomUUID());
        var runs = mock(IngestionRunStore.class); when(runs.ensureNewsJob(any())).thenReturn(java.util.UUID.randomUUID());
        when(runs.startAttempt(any(), any())).thenReturn(java.util.UUID.randomUUID());
        var service = new NewsIngestionService(properties, registry, sources, persistence, identity, runs,
                java.time.Clock.systemUTC());

        var result = service.ingestConfiguredSources();

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failedSources()).isEqualTo(2);
        verify(sources, times(2)).markFailure(any(), any(), any());
        verify(runs).finishFailedAttempt(any(), eq(0), any(), any());
    }

    private NewsProperties.Source source(String code, String url) {
        var source = new NewsProperties.Source();
        source.setCode(code); source.setName(code); source.setSourceType("RSS_ATOM"); source.setFeedUrl(url);
        return source;
    }
}
