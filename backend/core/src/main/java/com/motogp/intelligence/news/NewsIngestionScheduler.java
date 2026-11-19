package com.motogp.intelligence.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsIngestionScheduler {
    private static final Logger log = LoggerFactory.getLogger(NewsIngestionScheduler.class);
    private final NewsIngestionService ingestion;

    public NewsIngestionScheduler(NewsIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @Scheduled(fixedDelayString = "${motogp.news.scheduler.fixed-delay-ms:3600000}")
    public void ingest() {
        NewsIngestionService.NewsIngestionResult result = ingestion.ingestConfiguredSources();
        log.info("Scheduled news ingestion completed: {}", result);
    }
}
