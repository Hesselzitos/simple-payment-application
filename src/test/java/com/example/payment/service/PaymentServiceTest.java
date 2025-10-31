package com.example.payment.service;

import com.example.payment.domain.Payment;
import com.example.payment.domain.WebhookEvent;
import com.example.payment.domain.WebhookRegistration;
import com.example.payment.domain.WebhookStatus;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.WebhookEventRepository;
import com.example.payment.repository.WebhookRegistrationRepository;
import com.example.payment.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private WebhookRegistrationRepository webhookRegistrationRepository;
    private WebhookEventRepository webhookEventRepository;
    private EncryptionService encryptionService;
    private ObjectMapper objectMapper;

    private PaymentService service;

    @BeforeEach
    void setup() {
        paymentRepository = mock(PaymentRepository.class);
        webhookRegistrationRepository = mock(WebhookRegistrationRepository.class);
        webhookEventRepository = mock(WebhookEventRepository.class);
        encryptionService = mock(EncryptionService.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        service = new PaymentService(paymentRepository, webhookRegistrationRepository, webhookEventRepository, encryptionService, objectMapper);
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {
        @ParameterizedTest(name = "card: {0} -> last4: {1}")
        @CsvSource(value = {
                "123456789012,9012",
                "1234,1234",
                "999,",
                ",",
        }, nullValues = {"NULL", ""})
        void shouldMapFieldsAndSetCardLast4(String cardNumber, String expectedLast4) {
            // given
            given(encryptionService.encrypt(cardNumber)).willReturn(cardNumber == null ? null : "enc:" + cardNumber);
            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId("p1");
                return p;
            });
            given(webhookRegistrationRepository.findByActiveTrue()).willReturn(List.of());

            // when
            Payment p = service.createPayment("John", "Wick", "12345", cardNumber);

            // then
            assertEquals("p1", p.getId());
            assertEquals("John", p.getFirstName());
            assertEquals("Wick", p.getLastName());
            assertEquals("12345", p.getZipCode());
            assertEquals(expectedLast4, p.getCardLast4());
            if (cardNumber == null) {
                assertNull(p.getCardEncrypted());
            } else {
                assertEquals("enc:" + cardNumber, p.getCardEncrypted());
            }
            assertNotNull(p.getCreatedAt());
        }

        @Test
        void shouldEnqueueWebhookEventsForActiveRegistrations() throws Exception {
            // given
            String card = "5555444433332222";
            given(encryptionService.encrypt(card)).willReturn("enc");
            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId("pay-1");
                return p;
            });
            WebhookRegistration w1 = new WebhookRegistration();
            w1.setEndpointUrl("https://t1");
            WebhookRegistration w2 = new WebhookRegistration();
            w2.setEndpointUrl("https://t2");
            given(webhookRegistrationRepository.findByActiveTrue()).willReturn(List.of(w1, w2));

            ArgumentCaptor<WebhookEvent> evCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
            given(webhookEventRepository.save(any(WebhookEvent.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            Payment p = service.createPayment("Ana", "Maria", "12345", card);

            // then
            then(webhookEventRepository).should(times(2)).save(evCaptor.capture());
            List<WebhookEvent> events = evCaptor.getAllValues();
            assertEquals(2, events.size());
            for (WebhookEvent ev : events) {
                assertTrue(ev.getTargetUrl().startsWith("https://t"));
                assertEquals(WebhookStatus.PENDING, ev.getStatus());
                assertEquals(0, ev.getAttempts());
                assertNotNull(ev.getCreatedAt());
                assertNotNull(ev.getNextAttemptAt());
                assertNotNull(ev.getPayloadJson());
                assertTrue(ev.getPayloadJson().contains("payment.created"));
                assertTrue(ev.getPayloadJson().contains(p.getId()));
                assertFalse(ev.getPayloadJson().contains(card));
                assertTrue(ev.getPayloadJson().contains(p.getCardLast4()));
            }
        }

        @Test
        void shouldCatchSerializationErrorsAndStillReturnPayment() throws Exception {
            // given
            PaymentService svc = new PaymentService(paymentRepository, webhookRegistrationRepository, webhookEventRepository, encryptionService, new ObjectMapper() {
                @Override
                public String writeValueAsString(Object value) {
                    throw new RuntimeException("boom");
                }
            });
            given(encryptionService.encrypt("123456789012")).willReturn("enc");
            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId("pay-2");
                return p;
            });

            // when
            Payment p = svc.createPayment("A", "B", "00000", "123456789012");

            // then
            assertEquals("pay-2", p.getId());
            then(webhookEventRepository).should(never()).save(any());
        }
    }
}
