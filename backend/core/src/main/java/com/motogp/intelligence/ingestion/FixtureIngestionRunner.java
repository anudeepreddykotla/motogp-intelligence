package com.motogp.intelligence.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** One-shot operator command; inactive in every normal application profile. */
@Component
@Profile("fixture-ingestion")
@ConditionalOnProperty(name = "motogp.ingestion.run-fixture", havingValue = "true")
public class FixtureIngestionRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FixtureIngestionRunner.class);

    private final FixtureRefreshService refreshService;
    private final ConfigurableApplicationContext context;

    public FixtureIngestionRunner(FixtureRefreshService refreshService, ConfigurableApplicationContext context) {
        this.refreshService = refreshService;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            refreshService.refreshAndIngest(FixtureHistoricalDataProvider.CODE);
            log.info("Completed one-shot fixture refresh for {}", FixtureHistoricalDataProvider.CODE);
            context.close();
        } catch (RuntimeException exception) {
            log.error("Fixture refresh failed", exception);
            context.close();
            throw exception;
        }
    }
}
