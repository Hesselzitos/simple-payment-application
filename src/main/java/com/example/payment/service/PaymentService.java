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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final WebhookRegistrationRepository webhookRegistrationRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          WebhookRegistrationRepository webhookRegistrationRepository,
                          WebhookEventRepository webhookEventRepository,
                          EncryptionService encryptionService,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.webhookRegistrationRepository = webhookRegistrationRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment createPayment(String firstName, String lastName, String zipCode, String cardNumber) {
        Payment p = new Payment();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setZipCode(zipCode);
        p.setCreatedAt(Instant.now());
        p.setCardLast4(cardNumber != null && cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4) : null);
        p.setCardEncrypted(encryptionService.encrypt(cardNumber));
        p = paymentRepository.save(p);

        // Build webhook payload (no sensitive data)
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment.created");
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("firstName", p.getFirstName());
        data.put("lastName", p.getLastName());
        data.put("zipCode", p.getZipCode());
        data.put("cardLast4", p.getCardLast4());
        data.put("createdAt", p.getCreatedAt());
        payload.put("data", data);

        try {
            String json = objectMapper.writeValueAsString(payload);
            List<String> targets = webhookRegistrationRepository.findByActiveTrue()
                    .stream().map(WebhookRegistration::getEndpointUrl).toList();
            for (String target : targets) {
                WebhookEvent ev = new WebhookEvent();
                ev.setTargetUrl(target);
                ev.setPayloadJson(json);
                ev.setStatus(WebhookStatus.PENDING);
                ev.setAttempts(0);
                ev.setCreatedAt(Instant.now());
                ev.setNextAttemptAt(Instant.now());
                webhookEventRepository.save(ev);
            }
        } catch (Exception e) {
            log.error("Failed to enqueue webhook events for payment {}", p.getId(), e);
        }
        return p;
    }
}
