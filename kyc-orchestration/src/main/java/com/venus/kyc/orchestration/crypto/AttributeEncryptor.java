package com.venus.kyc.orchestration.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts String fields using AES-128/GCM.
 *
 * Storage format: Base64( IV[12 bytes] || ciphertext+tag[n+16 bytes] )
 *
 * The secret key is read from the property {@code encryption.secret-key} (must be exactly 16 bytes
 * for AES-128). In production, supply this via an environment variable or secrets manager rather
 * than hardcoding it.
 */
@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // Override via: encryption.secret-key in application.yml or ENCRYPTION_SECRET_KEY env var
    @Value("${encryption.secret-key:my-secret-key-12}")
    private String secretKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes());

            byte[] combined = new byte[GCM_IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH_BYTES, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting attribute", e);
        }
    }

    private SecretKeySpec buildKey() {
        return new SecretKeySpec(secretKey.getBytes(), "AES");
    }
}
