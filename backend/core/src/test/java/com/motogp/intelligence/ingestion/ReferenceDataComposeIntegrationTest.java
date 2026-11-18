package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url:jdbc:postgresql://localhost:5432/motogp}",
        "spring.datasource.username=${test.db.username:motogp}",
        "spring.datasource.password=${test.db.password:motogp}"
})
@EnabledIfSystemProperty(named = "runComposeIntegrationTests", matches = "true")
class ReferenceDataComposeIntegrationTest {
    @Autowired
    private ReferenceDataIngestionService ingestion;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void ingestsFixtureIntoComposePostgresAndIsIdempotent() {
        ReferenceIngestionResult first = ingestion.ingest(FixtureHistoricalDataProvider.CODE);
        ReferenceIngestionResult second = ingestion.ingest(FixtureHistoricalDataProvider.CODE);

        assertThat(first.recordsSeen()).isEqualTo(7);
        assertThat(second.recordsWritten()).isZero();
        assertThat(count("seasons")).isGreaterThanOrEqualTo(1);
        assertThat(count("manufacturers")).isGreaterThanOrEqualTo(1);
        assertThat(count("teams")).isGreaterThanOrEqualTo(1);
        assertThat(count("riders")).isGreaterThanOrEqualTo(2);
        assertThat(count("circuits")).isGreaterThanOrEqualTo(1);
        assertThat(count("events")).isGreaterThanOrEqualTo(1);
    }

    private long count(String table) {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
