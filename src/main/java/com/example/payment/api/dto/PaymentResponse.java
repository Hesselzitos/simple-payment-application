package com.example.payment.api.dto;

import com.example.payment.domain.Payment;
import lombok.Getter;

import java.time.Instant;

@Getter
public class PaymentResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String zipCode;
    private String cardLast4;
    private Instant createdAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.id = p.getId();
        r.firstName = p.getFirstName();
        r.lastName = p.getLastName();
        r.zipCode = p.getZipCode();
        r.cardLast4 = p.getCardLast4();
        r.createdAt = p.getCreatedAt();
        return r;
    }

}
