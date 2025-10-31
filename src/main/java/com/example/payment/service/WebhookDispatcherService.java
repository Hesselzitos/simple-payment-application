package com.example.payment.service;

import com.example.payment.domain.WebhookEvent;
import com.example.payment.domain.WebhookStatus;
import com.example.payment.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class WebhookDispatcherService {
    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcherService.class);

    private final WebhookEventRepository repository;
    private final HttpClient httpClient;

    private final boolean enabled;
    private final int maxAttempts;
    private final long baseBackoffMs;
    private final long maxBackoffMs;

    public WebhookDispatcherService(WebhookEventRepository repository,
                                    HttpClient httpClient,
                                    @Value("${webhook.dispatch.enabled:true}") boolean enabled,
                                    @Value("${webhook.dispatch.max-attempts:8}") int maxAttempts,
                                    @Value("${webhook.dispatch.base-backoff-ms:2000}") long baseBackoffMs,
                                    @Value("${webhook.dispatch.max-backoff-ms:120000}") long maxBackoffMs) {
        this.repository = repository;
        this.httpClient = httpClient;
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMs = baseBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    @Scheduled(fixedDelayString = "${webhook.dispatch.base-backoff-ms:2000}")
    public void dispatchLoop() {
        if (!enabled) return;
        List<WebhookEvent> due = repository.findByStatusInAndNextAttemptAtLessThanEqual(
                new ArrayList<>(EnumSet.of(WebhookStatus.PENDING, WebhookStatus.RETRY)), Instant.now());
        for (WebhookEvent ev : due) {
            tryDispatch(ev);
        }
    }

    private void tryDispatch(WebhookEvent ev) {
        ev.setLastAttemptAt(Instant.now());
        ev.setAttempts(ev.getAttempts() + 1);
        try {
            int attempt = ev.getAttempts();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ev.getTargetUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(ev.getPayloadJson()))
                    .build();

            int status;
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                status = response.statusCode();
            } catch (Exception ex) {
                log.warn("Webhook POST to {} failed: {}", ev.getTargetUrl(), ex.toString());
                status = 599; // custom network error
            }

            if (status >= 200 && status < 300) {
                ev.setStatus(WebhookStatus.DELIVERED);
                ev.setNextAttemptAt(null);
                ev.setLastError(null);
            } else {
                if (attempt >= maxAttempts) {
                    ev.setStatus(WebhookStatus.FAILED);
                    ev.setLastError("HTTP " + status);
                    ev.setNextAttemptAt(null);
                } else {
                    ev.setStatus(WebhookStatus.RETRY);
                    long backoff = Math.min(maxBackoffMs, (long) (baseBackoffMs * Math.pow(2, Math.max(0, attempt - 1))));
                    ev.setNextAttemptAt(Instant.now().plusMillis(backoff));
                    ev.setLastError("HTTP " + status);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error dispatching webhook {}: {}", ev.getId(), e.toString());
            if (ev.getAttempts() >= maxAttempts) {
                ev.setStatus(WebhookStatus.FAILED);
                ev.setNextAttemptAt(null);
                ev.setLastError(e.getMessage());
            } else {
                ev.setStatus(WebhookStatus.RETRY);
                long backoff = Math.min(maxBackoffMs, (long) (baseBackoffMs * Math.pow(2, Math.max(0, ev.getAttempts() - 1))));
                ev.setNextAttemptAt(Instant.now().plusMillis(backoff));
                ev.setLastError(e.getMessage());
            }
        } finally {
            repository.save(ev);
        }
    }
}
