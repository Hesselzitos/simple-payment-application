package com.example.payment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private static SecretKey newKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            return kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @DisplayName("encrypt/decrypt round-trip with various inputs")
    @ValueSource(strings = {
            "hello",
            "",
            "1234567890",
            "unicode-åß∂ƒ©",
            "multi line\nvalue"
    })
    void shouldEncryptAndDecryptRoundTrip(String input) {
        // given
        EncryptionService service = new EncryptionService(newKey());

        // when
        String enc = service.encrypt(input);
        String dec = service.decrypt(enc);

        // then
        assertNotNull(enc);
        assertFalse(enc.isBlank());
        assertEquals(input, dec);
        assertTrue(enc.contains(":"), "format should be iv:ciphertext");
    }
}
