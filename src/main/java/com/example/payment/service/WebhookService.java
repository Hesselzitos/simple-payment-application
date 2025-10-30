package com.example.payment.service;

import com.example.payment.domain.WebhookRegistration;
import com.example.payment.repository.WebhookRegistrationRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
public class WebhookService {
    private final WebhookRegistrationRepository repository;

    public WebhookService(WebhookRegistrationRepository repository) {
        this.repository = repository;
    }

    public WebhookRegistration register(String endpointUrl) {
        validateUrl(endpointUrl);
        if (repository.existsByEndpointUrl(endpointUrl)) {
            // idempotent: return existing or create active again
            List<WebhookRegistration> existing = repository.findAll()
                    .stream().filter(w -> endpointUrl.equals(w.getEndpointUrl())).toList();
            if (!existing.isEmpty()) {
                WebhookRegistration w = existing.getFirst();
                w.setActive(true);
                return repository.save(w);
            }
        }
        WebhookRegistration reg = new WebhookRegistration();
        reg.setEndpointUrl(endpointUrl);
        reg.setActive(true);
        reg.setCreatedAt(Instant.now());
        return repository.save(reg);
    }

    public List<WebhookRegistration> listActive() {
        return repository.findByActiveTrue();
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                throw new IllegalArgumentException("endpointUrl must be http or https");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("endpointUrl must include host");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid endpointUrl: " + e.getMessage());
        }
    }
}
