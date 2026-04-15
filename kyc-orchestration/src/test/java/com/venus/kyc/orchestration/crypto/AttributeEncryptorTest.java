package com.venus.kyc.orchestration.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AttributeEncryptorTest {

    private AttributeEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AttributeEncryptor();
        // Exactly 16 bytes for AES-128
        ReflectionTestUtils.setField(encryptor, "secretKey", "test-key-1234567");
    }

    @Test
    void convertToDatabaseColumn_null_returnsNull() {
        assertNull(encryptor.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_null_returnsNull() {
        assertNull(encryptor.convertToEntityAttribute(null));
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginalValue() {
        String original = "Jane Smith";
        String encrypted = encryptor.convertToDatabaseColumn(original);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_producesBase64Output() {
        String encrypted = encryptor.convertToDatabaseColumn("Jane Smith");
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }

    @Test
    void encrypt_sameInput_producedDifferentCiphertext() {
        String input = "Jane Smith";
        String first = encryptor.convertToDatabaseColumn(input);
        String second = encryptor.convertToDatabaseColumn(input);
        assertNotEquals(first, second, "Each encryption should use a different random IV");
    }

    @Test
    void decrypt_differentCiphertexts_returnSameOriginal() {
        String input = "Jane Smith";
        String first = encryptor.convertToDatabaseColumn(input);
        String second = encryptor.convertToDatabaseColumn(input);
        assertEquals(encryptor.convertToEntityAttribute(first),
                encryptor.convertToEntityAttribute(second));
    }
}
