package com.example.payment.config;

import com.example.payment.security.EncryptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class BeansConfig {

    @Bean
    public EncryptionService encryptionService(SecretKey encryptionKey) {
        return new EncryptionService(encryptionKey);
    }
}
