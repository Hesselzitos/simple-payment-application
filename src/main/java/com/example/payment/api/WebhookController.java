package com.example.payment.api;

import com.example.payment.api.dto.WebhookRegisterRequest;
import com.example.payment.domain.WebhookRegistration;
import com.example.payment.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<WebhookRegistration> register(@Valid @RequestBody WebhookRegisterRequest req) {
        WebhookRegistration reg = webhookService.register(req.getEndpointUrl());
        return ResponseEntity.status(201).body(reg);
    }

    @GetMapping
    public ResponseEntity<List<WebhookRegistration>> listActive() {
        return ResponseEntity.ok(webhookService.listActive());
    }
}
