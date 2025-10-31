package com.example.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private String validKeyBase64;

    @BeforeEach
    void setUp() {
        // Generate a random valid 256-bit key for testing
        byte[] keyBytes = new byte[32]; // 32 bytes = 256 bits
        for (int i = 0; i < keyBytes.length; i++) keyBytes[i] = (byte) i; // deterministic for test
        validKeyBase64 = Base64.getEncoder().encodeToString(keyBytes);

        encryptionService = new EncryptionService(validKeyBase64);
    }

    @Test
    void testEncryptDecrypt() {
        String plaintext = "Hello World!";
        String encrypted = encryptionService.encrypt(plaintext);

        assertNotNull(encrypted);
        assertTrue(encrypted.contains(":")); // iv:ciphertext format

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testInvalidKeyLength() {
        byte[] invalidKey = new byte[20];
        String invalidKeyBase64 = Base64.getEncoder().encodeToString(invalidKey);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new EncryptionService(invalidKeyBase64));
        assertTrue(exception.getMessage().contains("Invalid Base64 or AES key configuration"));
    }


    @Test
    void testDecryptInvalidFormat() {
        String invalidCiphertext = "this-is-not-valid";

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> encryptionService.decrypt(invalidCiphertext));
        assertTrue(exception.getMessage().contains("Decryption failure"));
    }

    @Test
    void testDecryptWithTamperedCiphertext() {
        String plaintext = "Sensitive data";
        String encrypted = encryptionService.encrypt(plaintext);

        // Tamper with ciphertext
        String tampered = encrypted.substring(0, encrypted.length() - 1) + "A";

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> encryptionService.decrypt(tampered));
        assertTrue(exception.getMessage().contains("Decryption failure"));
    }
}
