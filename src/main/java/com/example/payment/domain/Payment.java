package com.example.payment.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Setter
@Getter
@Document("payments")
public class Payment {
    @Id
    private String id;

    private String firstName;
    private String lastName;
    private String zipCode;

    // Encrypted card number (AES-GCM; format: Base64(IV):Base64(ciphertext))
    private String cardEncrypted;

    // Convenience non-sensitive field
    private String cardLast4;

    private Instant createdAt;

}
