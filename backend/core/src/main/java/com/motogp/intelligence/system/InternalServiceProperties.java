package com.motogp.intelligence.system;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "motogp.internal-ai")
public record InternalServiceProperties(String baseUrl, String token) {
}
