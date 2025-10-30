package com.example.payment.repository;

import com.example.payment.domain.WebhookEvent;
import com.example.payment.domain.WebhookStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface WebhookEventRepository extends MongoRepository<WebhookEvent, String> {
    List<WebhookEvent> findByStatusInAndNextAttemptAtLessThanEqual(List<WebhookStatus> statuses, Instant nextAttemptAt);
}
