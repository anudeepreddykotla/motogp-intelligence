package com.motogp.intelligence.news;

import com.motogp.intelligence.ingestion.IdentityMappingStore;
import com.motogp.intelligence.ingestion.IngestionRunStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NewsIngestionService {
    public static final String PROVIDER_CODE = "news-rss";

    private final NewsProperties properties;
    private final NewsSourceAdapterRegistry adapters;
    private final NewsSourceStore sources;
    private final NewsArticlePersistenceService persistence;
    private final IdentityMappingStore identityStore;
    private final IngestionRunStore ingestionRuns;
    private final Clock clock;

    @Autowired
    public NewsIngestionService(NewsProperties properties, NewsSourceAdapterRegistry adapters,
                                NewsSourceStore sources, NewsArticlePersistenceService persistence,
                                IdentityMappingStore identityStore, IngestionRunStore ingestionRuns) {
        this(properties, adapters, sources, persistence, identityStore, ingestionRuns, Clock.systemUTC());
    }

    NewsIngestionService(NewsProperties properties, NewsSourceAdapterRegistry adapters, NewsSourceStore sources,
                         NewsArticlePersistenceService persistence, IdentityMappingStore identityStore,
                         IngestionRunStore ingestionRuns, Clock clock) {
        this.properties = properties;
        this.adapters = adapters;
        this.sources = sources;
        this.persistence = persistence;
        this.identityStore = identityStore;
        this.ingestionRuns = ingestionRuns;
        this.clock = clock;
    }

    public NewsIngestionResult ingestConfiguredSources() {
        UUID jobId = ingestionRuns.ensureNewsJob(PROVIDER_CODE);
        UUID attemptId = ingestionRuns.startAttempt(jobId, Instant.now(clock));
        UUID providerId = identityStore.providerId(PROVIDER_CODE);
        int recordsSeen = 0;
        int recordsWritten = 0;
        int successfulSources = 0;
        int failedSources = 0;
        Set<String> errors = new HashSet<>();
        for (NewsProperties.Source configured : properties.getSources()) {
            if (configured.getCode() == null || configured.getName() == null || configured.getFeedUrl() == null) {
                failedSources++;
                errors.add("News source configuration is missing code, name, or feedUrl");
                continue;
            }
            NewsSourceDefinition source = configured.definition();
            UUID sourceId = null;
            try {
                sourceId = sources.upsert(source, providerId);
                Instant attemptedAt = Instant.now(clock);
                sources.markAttempt(sourceId, attemptedAt);
                if (!source.enabled()) continue;
                List<NewsArticleRecord> fetched = adapters.require(source.sourceType()).fetch(source);
                List<NewsArticleRecord> unique = fetched.stream()
                        .collect(java.util.stream.Collectors.toMap(NewsArticleRecord::deduplicationKey,
                                item -> item, (first, second) -> first)).values().stream().toList();
                recordsSeen += fetched.size();
                recordsWritten += persistence.persist(providerId, sourceId, unique, Instant.now(clock));
                sources.markSuccess(sourceId, Instant.now(clock));
                successfulSources++;
            } catch (RuntimeException exception) {
                failedSources++;
                errors.add(source.code() + ": " + summarize(exception));
                if (sourceId != null) sources.markFailure(sourceId, Instant.now(clock), summarize(exception));
            }
        }
        String status = failedSources == 0 ? "SUCCEEDED" : successfulSources == 0 ? "FAILED" : "PARTIAL";
        if ("FAILED".equals(status)) {
            ingestionRuns.finishFailedAttempt(attemptId, recordsSeen, String.join("; ", errors), Instant.now(clock));
        } else {
            ingestionRuns.finishAttempt(attemptId, status, recordsSeen, recordsWritten, Instant.now(clock));
        }
        return new NewsIngestionResult(recordsSeen, recordsWritten, successfulSources, failedSources, status);
    }

    private String summarize(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public record NewsIngestionResult(int recordsSeen, int recordsWritten, int successfulSources,
                                      int failedSources, String status) { }
}
