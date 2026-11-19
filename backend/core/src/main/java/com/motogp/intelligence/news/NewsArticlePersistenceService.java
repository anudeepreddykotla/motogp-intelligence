package com.motogp.intelligence.news;

import com.motogp.intelligence.ingestion.IdentityMappingStore;
import com.motogp.intelligence.ingestion.ReferenceEntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NewsArticlePersistenceService {
    private final IdentityMappingStore identityStore;
    private final NewsArticleStore articles;

    public NewsArticlePersistenceService(IdentityMappingStore identityStore, NewsArticleStore articles) {
        this.identityStore = identityStore;
        this.articles = articles;
    }

    @Transactional
    public int persist(UUID providerId, UUID sourceId, List<NewsArticleRecord> records, Instant fetchedAt) {
        int written = 0;
        for (NewsArticleRecord record : records) {
            UUID sourceRecordId = identityStore.upsertSourceRecord(providerId, ReferenceEntityType.NEWS_ARTICLE,
                    record.deduplicationKey(), null, fetchedAt);
            if (articles.upsert(sourceId, sourceRecordId, record, fetchedAt)) written++;
        }
        return written;
    }
}
