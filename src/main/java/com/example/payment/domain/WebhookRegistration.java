package com.example.payment.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Setter
@Getter
@Document("webhook_registrations")
public class WebhookRegistration {
    @Id
    private String id;

    @Indexed(unique = true)
    private String endpointUrl;

    private boolean active = true;

    private Instant createdAt;

}
