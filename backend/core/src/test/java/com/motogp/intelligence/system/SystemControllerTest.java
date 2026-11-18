package com.motogp.intelligence.system;

import com.motogp.intelligence.common.CorrelationIdFilter;
import com.motogp.intelligence.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, GlobalExceptionHandler.class})
class SystemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    PythonAiClient pythonAiClient;

    @Test
    void statusReturnsCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER))
                .andExpect(jsonPath("$.data.service").value("java-core"));
    }

    @Test
    void validationRejectsBlankValue() throws Exception {
        mockMvc.perform(post("/api/v1/system/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"));
    }

    @Test
    void pythonIntegrationReturnsTypedHealth() throws Exception {
        given(pythonAiClient.health()).willReturn(
                new PythonAiClient.PythonHealthResponse("motogp-ai", "UP", "1.0", null));

        mockMvc.perform(get("/api/v1/system/integration/python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("motogp-ai"))
                .andExpect(jsonPath("$.data.contractVersion").value("1.0"));
    }
}
