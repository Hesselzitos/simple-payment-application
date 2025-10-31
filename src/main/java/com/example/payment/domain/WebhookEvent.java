package com.example.payment.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Setter
@Getter
@Document("webhook_events")
public class WebhookEvent {
    @Id
    private String id;

    private String targetUrl;

    private String payloadJson;

    private WebhookStatus status = WebhookStatus.PENDING;

    private int attempts;

    @Indexed
    private Instant nextAttemptAt;

    private Instant lastAttemptAt;

    private String lastError;

    private Instant createdAt;

}
