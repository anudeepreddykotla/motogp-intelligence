package com.motogp.intelligence.system;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(InternalServiceProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/api/v1/system/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/seasons", "/api/v1/seasons/*",
                                "/api/v1/events", "/api/v1/events/*", "/api/v1/events/*/sessions",
                                "/api/v1/riders/compare",
                                "/api/v1/sessions/*", "/api/v1/sessions/*/results",
                                "/api/v1/sessions/*/laps", "/api/v1/sessions/*/positions",
                                "/api/v1/news", "/api/v1/news/*").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
