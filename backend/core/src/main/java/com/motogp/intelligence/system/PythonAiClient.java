package com.motogp.intelligence.system;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class PythonAiClient {
    private final RestClient restClient;
    private final InternalServiceProperties properties;

    public PythonAiClient(RestClient.Builder builder, InternalServiceProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public PythonHealthResponse health() {
        return restClient.get()
                .uri("/internal/v1/health")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(PythonHealthResponse.class);
    }

    public record PythonHealthResponse(String service, String status, String contractVersion,
                                       Instant checkedAt) {
    }
}
