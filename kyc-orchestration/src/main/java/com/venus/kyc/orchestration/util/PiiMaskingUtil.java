package com.venus.kyc.orchestration.util;

/**
 * Utility for masking PII in log output (KYC-NF-12).
 * Never log raw firstName, lastName, or dob — always pass through mask() first.
 */
public final class PiiMaskingUtil {

    private PiiMaskingUtil() {}

    /**
     * Returns a masked representation suitable for logs.
     * e.g. "Smith" → "S***h", "Jo" → "J***o", null → "[null]"
     */
    public static String mask(String value) {
        if (value == null) return "[null]";
        if (value.length() <= 2) return value.charAt(0) + "***";
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }
}
