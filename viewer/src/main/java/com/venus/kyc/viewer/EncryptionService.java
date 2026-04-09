package com.venus.kyc.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for PII fields.
 *
 * <p>Encrypted format: Base64(iv[12] + ciphertext + authTag[16])
 * The IV is randomly generated per-encryption and prepended to the ciphertext.
 * Null and blank values are passed through unmodified.
 *
 * <p>The encryption key must be exactly 32 bytes (256 bits) encoded as Base64
 * in the {@code app.encryptionKey} property.
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public EncryptionService(@Value("${app.encryptionKey}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "app.encryptionKey must be a Base64-encoded 32-byte (256-bit) key, got " + keyBytes.length + " bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptionService initialized with AES-256-GCM");
    }

    /**
     * Encrypts a plaintext string. Returns null if the input is null or blank.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts an encrypted string. Returns null if the input is null or blank.
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return encrypted;
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), "UTF-8");
        } catch (Exception e) {
            log.warn("Decryption failed — returning raw value (may be unencrypted legacy data)");
            return encrypted;
        }
    }
}
