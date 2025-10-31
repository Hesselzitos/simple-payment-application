package com.example.payment.api;

import com.example.payment.api.dto.WebhookRegisterRequest;
import com.example.payment.domain.WebhookRegistration;
import com.example.payment.service.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@Import(WebhookControllerTest.TestConfig.class)
class WebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WebhookService webhookService;

    static class TestConfig {
        @Bean
        public WebhookService webhookService() {
            return mock(WebhookService.class);
        }
    }

    @Test
    void shouldListActive() throws Exception {
        WebhookRegistration a = new WebhookRegistration();
        a.setId("a");
        a.setEndpointUrl("https://a");
        a.setActive(true);
        a.setCreatedAt(Instant.now());
        WebhookRegistration b = new WebhookRegistration();
        b.setId("b");
        b.setEndpointUrl("https://b");
        b.setActive(true);
        b.setCreatedAt(Instant.now());
        given(webhookService.listActive()).willReturn(List.of(a, b));

        mockMvc.perform(get("/api/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].endpointUrl", is("https://a")))
                .andExpect(jsonPath("$[1].endpointUrl", is("https://b")));
    }

    @Nested
    @DisplayName("POST /api/webhooks")
    class RegisterWebhook {
        @Test
        void shouldRegister() throws Exception {
            WebhookRegistration reg = new WebhookRegistration();
            reg.setId("w1");
            reg.setEndpointUrl("https://example.com/hook");
            reg.setActive(true);
            reg.setCreatedAt(Instant.now());
            given(webhookService.register(anyString())).willReturn(reg);

            WebhookRegisterRequest req = new WebhookRegisterRequest();
            req.setEndpointUrl("https://example.com/hook");

            mockMvc.perform(post("/api/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is("w1")))
                    .andExpect(jsonPath("$.endpointUrl", is("https://example.com/hook")))
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @ParameterizedTest(name = "invalid endpoint: {0}")
        @ValueSource(strings = {"", "ftp://x", "http://", "not-a-url"})
        void shouldValidateBadUrl(String url) throws Exception {
            WebhookRegisterRequest req = new WebhookRegisterRequest();
            req.setEndpointUrl(url);

            // Service performs URL validation and throws IllegalArgumentException
            if (!url.isBlank()) {
                given(webhookService.register(url))
                        .willThrow(new IllegalArgumentException("Invalid endpointUrl: bad url"));
            }

            mockMvc.perform(post("/api/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", anyOf(containsString("Validation"), containsString("Invalid endpointUrl"))));
        }
    }
}
