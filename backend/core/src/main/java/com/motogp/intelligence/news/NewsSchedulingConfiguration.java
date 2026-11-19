package com.motogp.intelligence.news;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "motogp.news.scheduler.enabled", havingValue = "true")
public class NewsSchedulingConfiguration {
}
