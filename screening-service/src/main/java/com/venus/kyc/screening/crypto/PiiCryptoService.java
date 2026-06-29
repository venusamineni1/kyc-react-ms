package com.venus.kyc.screening.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts/decrypts sensitive String columns using AES-128/GCM before they are bound to raw
 * JDBC params (screening-service has no JPA/Hibernate, so it can't use a JPA AttributeConverter
 * the way kyc-orchestration's AttributeEncryptor does — this is the same scheme, applied manually
 * at the repository boundary instead).
 *
 * Storage format: Base64( IV[12 bytes] || ciphertext+tag[n+16 bytes] )
 *
 * The secret key is read from {@code encryption.secret-key} / ENCRYPTION_SECRET_KEY so it can be
 * shared operationally with kyc-orchestration's AttributeEncryptor (must be exactly 16 bytes for
 * AES-128). Override the default in production.
 */
@Component
public class PiiCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Value("${encryption.secret-key:my-secret-key-12}")
    private String secretKey;

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] combined = new byte[GCM_IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH_BYTES, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting attribute", e);
        }
    }

    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);
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
