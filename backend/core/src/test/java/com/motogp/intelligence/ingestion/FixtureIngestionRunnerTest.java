package com.motogp.intelligence.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureIngestionRunnerTest {
    @Test
    void runnerIsRestrictedToExplicitFixtureProfileAndProperty() {
        assertThat(FixtureIngestionRunner.class.getAnnotation(Profile.class).value())
                .containsExactly("fixture-ingestion");
        assertThat(FixtureIngestionRunner.class.getAnnotation(ConditionalOnProperty.class).havingValue())
                .isEqualTo("true");
        assertThat(FixtureIngestionRunner.class.getAnnotation(ConditionalOnProperty.class).name())
                .containsExactly("motogp.ingestion.run-fixture");
    }
}
