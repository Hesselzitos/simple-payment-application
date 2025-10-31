package com.example.payment.api;

import com.example.payment.api.dto.PaymentRequest;
import com.example.payment.api.dto.PaymentResponse;
import com.example.payment.domain.Payment;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest req,
                                                  UriComponentsBuilder uriBuilder) {
        Payment p = paymentService.createPayment(req.getFirstName(), req.getLastName(), req.getZipCode(), req.getCardNumber());
        PaymentResponse resp = PaymentResponse.from(p);
        return ResponseEntity.created(uriBuilder.path("/api/payments/{id}").buildAndExpand(p.getId()).toUri())
                .body(resp);
    }
}
