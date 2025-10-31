package com.example.payment.api;

import com.example.payment.api.dto.PaymentRequest;
import com.example.payment.domain.Payment;
import com.example.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(PaymentControllerTest.TestConfig.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentService paymentService;

    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public PaymentService paymentService() {
            return org.mockito.Mockito.mock(PaymentService.class);
        }
    }

    private static Stream<PaymentRequest> invalidRequests() {
        PaymentRequest r1 = new PaymentRequest();
        r1.setFirstName(""); // blank
        r1.setLastName("Doe");
        r1.setZipCode("12345");
        r1.setCardNumber("123456789012");

        PaymentRequest r2 = new PaymentRequest();
        r2.setFirstName("John");
        r2.setLastName(""); // blank
        r2.setZipCode("12345");
        r2.setCardNumber("123456789012");

        PaymentRequest r3 = new PaymentRequest();
        r3.setFirstName("John");
        r3.setLastName("Doe");
        r3.setZipCode("bad-zip!"); // pattern fail
        r3.setCardNumber("123456789012");

        PaymentRequest r4 = new PaymentRequest();
        r4.setFirstName("John");
        r4.setLastName("Doe");
        r4.setZipCode("123"); // size fail
        r4.setCardNumber("123456789012");

        PaymentRequest r5 = new PaymentRequest();
        r5.setFirstName("John");
        r5.setLastName("Doe");
        r5.setZipCode("12345");
        r5.setCardNumber("abcd"); // digits pattern fail

        return Stream.of(r1, r2, r3, r4, r5);
    }

    @Nested
    @DisplayName("POST /api/payments")
    class CreatePayment {
        @Test
        @DisplayName("should create a payment and return 201 with response and Location header")
        void shouldCreate() throws Exception {
            // given
            Payment p = new Payment();
            p.setId("id-123");
            p.setFirstName("Jane");
            p.setLastName("Doe");
            p.setZipCode("12345");
            p.setCardLast4("4242");
            p.setCreatedAt(Instant.now());
            given(paymentService.createPayment(anyString(), anyString(), anyString(), anyString())).willReturn(p);

            PaymentRequest req = new PaymentRequest();
            req.setFirstName("Jane");
            req.setLastName("Doe");
            req.setZipCode("12345");
            req.setCardNumber("4242424242424242");

            // when/then
            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/payments/id-123")))
                    .andExpect(jsonPath("$.id", is("id-123")))
                    .andExpect(jsonPath("$.firstName", is("Jane")))
                    .andExpect(jsonPath("$.cardLast4", is("4242")));
        }

        @ParameterizedTest(name = "invalid request -> {index}")
        @MethodSource("com.example.payment.api.PaymentControllerTest#invalidRequests")
        void shouldValidate(PaymentRequest invalid) throws Exception {
            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation")))
                    .andExpect(jsonPath("$.errors", aMapWithSize(greaterThanOrEqualTo(1))));
        }
    }
}
