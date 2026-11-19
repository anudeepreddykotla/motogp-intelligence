package com.motogp.intelligence.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("news-ingestion")
@ConditionalOnProperty(name = "motogp.news.run", havingValue = "true")
public class NewsIngestionRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NewsIngestionRunner.class);

    private final NewsIngestionService ingestion;
    private final ConfigurableApplicationContext context;

    public NewsIngestionRunner(NewsIngestionService ingestion, ConfigurableApplicationContext context) {
        this.ingestion = ingestion;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            NewsIngestionService.NewsIngestionResult result = ingestion.ingestConfiguredSources();
            log.info("Completed news ingestion: {}", result);
            context.close();
        } catch (RuntimeException exception) {
            log.error("News ingestion failed", exception);
            context.close();
            throw exception;
        }
    }
}
