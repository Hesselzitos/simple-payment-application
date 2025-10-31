package com.example.payment.config;

import com.example.payment.security.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Value("${ENCRYPTION_KEY}")
    private String encryptionKey;

    @Bean
    public EncryptionService encryptionService() {
        return new EncryptionService(encryptionKey);
    }
}
