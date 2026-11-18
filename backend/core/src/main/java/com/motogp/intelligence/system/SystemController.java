package com.motogp.intelligence.system;

import com.motogp.intelligence.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
@Validated
public class SystemController {
    private final PythonAiClient pythonAiClient;

    public SystemController(PythonAiClient pythonAiClient) {
        this.pythonAiClient = pythonAiClient;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatus> status() {
        return ApiResponse.success(requestId(), new SystemStatus("java-core", "UP", Instant.now()));
    }

    @GetMapping("/integration/python")
    public ApiResponse<PythonAiClient.PythonHealthResponse> pythonHealth() {
        return ApiResponse.success(requestId(), pythonAiClient.health());
    }

    @PostMapping("/validation")
    public ApiResponse<ValidationResponse> validation(@Valid @RequestBody ValidationRequest request) {
        return ApiResponse.success(requestId(), new ValidationResponse(request.value()));
    }

    private String requestId() {
        return MDC.get(com.motogp.intelligence.common.CorrelationIdFilter.MDC_KEY);
    }

    public record SystemStatus(String service, String status, Instant checkedAt) {
    }

    public record ValidationRequest(@NotBlank String value) {
    }

    public record ValidationResponse(String value) {
    }
}
