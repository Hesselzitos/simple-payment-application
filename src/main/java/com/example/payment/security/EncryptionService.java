package com.example.payment.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption/decryption service.
 * Expects a Base64-encoded AES key (128/192/256 bits) to be provided.
 */
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes tag
    private static final int IV_LENGTH_BYTES = 12;      // recommended for GCM

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates an EncryptionService using a Base64-encoded AES key.
     * The decoded key must be 16, 24, or 32 bytes long.
     *
     * @param base64Key Base64-encoded AES key
     */
    public EncryptionService(String base64Key) {
        this.key = loadKey(base64Key);
    }

    private SecretKey loadKey(String base64Key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            int len = decoded.length;
            if (len != 16 && len != 24 && len != 32) {
                throw new IllegalArgumentException("Invalid AES key length: " + len + " bytes. Must be 16, 24, or 32.");
            }
            return new SecretKeySpec(decoded, ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 or AES key configuration", e);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // format: iv:ciphertext (both Base64)
            return Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failure", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            String[] parts = encoded.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid ciphertext format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ct = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failure", e);
        }
    }
}
