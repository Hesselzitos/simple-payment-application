package com.example.payment.repository;

import com.example.payment.domain.WebhookRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WebhookRegistrationRepository extends MongoRepository<WebhookRegistration, String> {
    List<WebhookRegistration> findByActiveTrue();

    boolean existsByEndpointUrl(String endpointUrl);
}
