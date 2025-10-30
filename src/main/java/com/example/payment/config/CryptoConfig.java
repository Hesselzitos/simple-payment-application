package com.example.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class CryptoConfig {

    @Bean
    public SecretKey encryptionKey(@Value("${payment.encryption.secret}") String base64) {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        // Accept 16/24/32-byte keys; recommend 32 bytes for AES-256
        return new SecretKeySpec(keyBytes, "AES");
    }
}
