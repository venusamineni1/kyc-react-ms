package com.venus.kyc.document.service;

import com.venus.kyc.document.model.OcrField;
import com.venus.kyc.document.model.OcrResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ICAO 9303 Machine Readable Zones (MRZ) from raw text.
 * Supports:
 *   TD3 — Passport              (2 lines × 44 chars)
 *   TD2 — Official Travel Doc   (2 lines × 36 chars)
 *   TD1 — ID Card               (3 lines × 30 chars)
 * Also validates check digits per ICAO 9303 Part 3.
 */
@Service
public class MrzParserService {

    // Weights used in ICAO check-digit algorithm: repeating 7, 3, 1
    private static final int[] WEIGHTS = {7, 3, 1};

    // Regex patterns to locate MRZ zones in OCR'd text.
    // TD3 (passport): 2 lines × 44 chars each.
    //   Line 1: positions 0-43 — doc type, subtype, issuer (3), name (39) — A-Z and < only
    //   Line 2: positions 0-43 — doc number, check, nationality, DOB, check, sex, expiry, check, personal, composite check — A-Z, 0-9 and <
    private static final Pattern TD3_PATTERN = Pattern.compile(
        "([A-Z<]{44})\n([A-Z0-9<]{44})",
        Pattern.MULTILINE
    );
    // TD2 (official travel doc): 2 lines × 36 chars each.
    //   Line 1: doc type (1), subtype (1), issuer (3), name (31) — A-Z and < only
    //   Line 2: doc number (9), check, nationality (3), DOB (6), check, sex, expiry (6), check, optional (7), composite check — A-Z, 0-9 and <
    private static final Pattern TD2_PATTERN = Pattern.compile(
        "([A-Z<]{36})\n([A-Z0-9<]{36})",
        Pattern.MULTILINE
    );
    // TD1 (ID card / permanent resident): 3 lines × 30 chars each.
    //   Line 1 pos 0 must be a letter (doc type); pos 1 may be a letter, digit, or < per ICAO 9303.
    //   (e.g. US Green Card uses "C1" — the "1" at pos 1 is a digit, not a letter.)
    private static final Pattern TD1_PATTERN = Pattern.compile(
        "([A-Z][A-Z0-9<]{29})\n([0-9A-Z<]{30})\n([A-Z<]{30})",
        Pattern.MULTILINE
    );

    /** Try to locate and parse MRZ from raw text. Returns null if not found. */
    public OcrResult parseMrzFromText(String text) {
        if (text == null || text.isBlank()) return null;

        // Normalise: remove spaces within MRZ candidate lines
        String normalised = normaliseMrzText(text);

        // Try TD3 (passport, 2×44) first — most common
        Matcher td3 = TD3_PATTERN.matcher(normalised);
        if (td3.find()) {
            return parseTD3(td3.group(1), td3.group(2));
        }

        // Try TD2 (official travel doc, 2×36)
        Matcher td2 = TD2_PATTERN.matcher(normalised);
        if (td2.find()) {
            return parseTD2(td2.group(1), td2.group(2));
        }

        // Try TD1 (ID card, 3×30)
        Matcher td1 = TD1_PATTERN.matcher(normalised);
        if (td1.find()) {
            return parseTD1(td1.group(1), td1.group(2), td1.group(3));
        }

        // Fuzzy fallback: exact-length regex failed, likely due to OCR inserting
        // or dropping a character. Find MRZ-valid lines, try all ±2-char substrings,
        // and use check-digit validation to pick the correct window.
        return parseMrzFuzzy(normalised);
    }

    // ── Fuzzy MRZ search ─────────────────────────────────────────────────────

    private OcrResult parseMrzFuzzy(String text) {
        // Collect lines that consist entirely of MRZ-valid characters (A-Z, 0-9, <)
        List<String> mrzLines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (!line.isEmpty() && line.matches("[A-Z0-9<]+")) {
                mrzLines.add(line);
            }
        }

        // Try each pair / triple of consecutive MRZ-valid lines
        for (int i = 0; i < mrzLines.size(); i++) {
            // TD3: 2 × 44
            if (i + 1 < mrzLines.size()) {
                for (String l1 : lengthCandidates(mrzLines.get(i),   44)) {
                for (String l2 : lengthCandidates(mrzLines.get(i+1), 44)) {
                    OcrResult r = parseTD3(l1, l2);
                    if ("PASS".equals(r.getMrzCheckDigits())) return r;
                }}
            }
            // TD2: 2 × 36
            if (i + 1 < mrzLines.size()) {
                for (String l1 : lengthCandidates(mrzLines.get(i),   36)) {
                for (String l2 : lengthCandidates(mrzLines.get(i+1), 36)) {
                    OcrResult r = parseTD2(l1, l2);
                    if ("PASS".equals(r.getMrzCheckDigits())) return r;
                }}
            }
            // TD1: 3 × 30
            if (i + 2 < mrzLines.size()) {
                for (String l1 : lengthCandidates(mrzLines.get(i),   30)) {
                for (String l2 : lengthCandidates(mrzLines.get(i+1), 30)) {
                for (String l3 : lengthCandidates(mrzLines.get(i+2), 30)) {
                    OcrResult r = parseTD1(l1, l2, l3);
                    if ("PASS".equals(r.getMrzCheckDigits())) return r;
                }}}
            }
        }
        return null;
    }

    /**
     * Returns all substrings of {@code line} that are exactly {@code targetLen} chars,
     * within ±2 chars tolerance. Returns empty list if the line length is too far off.
     */
    private List<String> lengthCandidates(String line, int targetLen) {
        int len = line.length();
        if (Math.abs(len - targetLen) > 2) return List.of();
        if (len == targetLen) return List.of(line);
        // Line is 1-2 chars too long: enumerate every targetLen-char window
        List<String> result = new ArrayList<>();
        for (int start = 0; start <= len - targetLen; start++) {
            result.add(line.substring(start, start + targetLen));
        }
        return result;
    }

    /** Parse a raw MRZ string (lines separated by newline). */
    public OcrResult parseRawMrz(String mrzText) {
        if (mrzText == null) return null;
        String[] lines = mrzText.trim().split("\n");
        if (lines.length == 2 && lines[0].length() == 44) {
            return parseTD3(lines[0], lines[1]);
        }
        if (lines.length == 2 && lines[0].length() == 36) {
            return parseTD2(lines[0], lines[1]);
        }
        if (lines.length == 3 && lines[0].length() == 30) {
            return parseTD1(lines[0], lines[1], lines[2]);
        }
        return null;
    }

    // ── TD3 — Passport ───────────────────────────────────────────────────────
    private OcrResult parseTD3(String line1, String line2) {
        OcrResult result = new OcrResult();
        result.setRawMrz(line1 + "\n" + line2);

        // Line 1 — P<ISSUING_COUNTRYNAME<<GIVENNAMES...
        if (line1.length() >= 44) {
            String docType   = line1.substring(0, 1).replace("<", "").trim();
            String issuer    = line1.substring(2, 5).replace("<", "").trim();
            String nameField = line1.substring(5, 44);
            String[] nameParts = nameField.split("<<");
            String surname   = nameParts.length > 0 ? nameParts[0].replace("<", " ").trim() : "";
            String given     = nameParts.length > 1 ? nameParts[1].replace("<", " ").trim() : "";

            result.setDocumentType(new OcrField(docType, 1.0, "MRZ"));
            result.setIssuingCountry(new OcrField(issuer, 1.0, "MRZ"));
            result.setSurname(new OcrField(surname, 0.99, "MRZ"));
            result.setGivenNames(new OcrField(given, 0.99, "MRZ"));
        }

        // Line 2 — DOCNUMBER CHECK NATIONALITY DOB CHECK SEX EXPIRY CHECK PERSONAL CHECK COMPOSITE CHECK
        if (line2.length() >= 44) {
            String docNumber    = line2.substring(0, 9);
            char   docNumCheck  = line2.charAt(9);
            String nationality  = line2.substring(10, 13).replace("<", "").trim();
            String dob          = line2.substring(13, 19);
            char   dobCheck     = line2.charAt(19);
            String sex          = line2.substring(20, 21);
            String expiry       = line2.substring(21, 27);
            char   expiryCheck  = line2.charAt(27);
            String personal     = line2.substring(28, 42).replace("<", "").trim();

            result.setNationality(new OcrField(nationality, 1.0, "MRZ"));
            result.setDateOfBirth(new OcrField(formatDate(dob), 0.99, "MRZ"));
            result.setExpiryDate(new OcrField(formatDate(expiry), 0.99, "MRZ"));
            result.setDocumentNumber(new OcrField(docNumber.replace("<", "").trim(), 0.99, "MRZ"));
            if (!personal.isBlank()) {
                result.setPersonalNumber(new OcrField(personal, 0.95, "MRZ"));
            }

            // Validate check digits
            boolean docNumValid = validateCheckDigit(docNumber, docNumCheck);
            boolean dobValid    = validateCheckDigit(dob, dobCheck);
            boolean expiryValid = validateCheckDigit(expiry, expiryCheck);

            result.setMrzCheckDigits(docNumValid && dobValid && expiryValid ? "PASS" : "FAIL");
        }

        result.setSource("MRZ");
        return result;
    }

    // ── TD2 — Official Travel Document ───────────────────────────────────────
    private OcrResult parseTD2(String line1, String line2) {
        OcrResult result = new OcrResult();
        result.setRawMrz(line1 + "\n" + line2);

        // Line 1: doc type (0), subtype (1), issuer (2-4), name (5-35)
        if (line1.length() >= 36) {
            String docType   = line1.substring(0, 2).replace("<", "").trim();
            String issuer    = line1.substring(2, 5).replace("<", "").trim();
            String nameField = line1.substring(5, 36);
            String[] parts   = nameField.split("<<");
            String surname   = parts.length > 0 ? parts[0].replace("<", " ").trim() : "";
            String given     = parts.length > 1 ? parts[1].replace("<", " ").trim() : "";

            result.setDocumentType(new OcrField(docType, 1.0, "MRZ"));
            result.setIssuingCountry(new OcrField(issuer, 1.0, "MRZ"));
            result.setSurname(new OcrField(surname, 0.99, "MRZ"));
            result.setGivenNames(new OcrField(given, 0.99, "MRZ"));
        }

        // Line 2: doc number (0-8), check (9), nationality (10-12),
        //         DOB (13-18), check (19), sex (20), expiry (21-26),
        //         check (27), optional (28-34), composite check (35)
        if (line2.length() >= 36) {
            String docNumber   = line2.substring(0, 9);
            char   docNumCheck = line2.charAt(9);
            String nationality = line2.substring(10, 13).replace("<", "").trim();
            String dob         = line2.substring(13, 19);
            char   dobCheck    = line2.charAt(19);
            String expiry      = line2.substring(21, 27);
            char   expiryCheck = line2.charAt(27);

            result.setDocumentNumber(new OcrField(docNumber.replace("<", "").trim(), 0.99, "MRZ"));
            result.setNationality(new OcrField(nationality, 1.0, "MRZ"));
            result.setDateOfBirth(new OcrField(formatDate(dob), 0.99, "MRZ"));
            result.setExpiryDate(new OcrField(formatDate(expiry), 0.99, "MRZ"));

            boolean docNumValid = validateCheckDigit(docNumber, docNumCheck);
            boolean dobValid    = validateCheckDigit(dob, dobCheck);
            boolean expiryValid = validateCheckDigit(expiry, expiryCheck);
            result.setMrzCheckDigits(docNumValid && dobValid && expiryValid ? "PASS" : "FAIL");
        }

        result.setSource("MRZ");
        return result;
    }

    // ── TD1 — ID Card ────────────────────────────────────────────────────────
    private OcrResult parseTD1(String line1, String line2, String line3) {
        OcrResult result = new OcrResult();
        result.setRawMrz(line1 + "\n" + line2 + "\n" + line3);

        if (line1.length() >= 30) {
            String docType  = line1.substring(0, 2).replace("<", "").trim();
            String issuer   = line1.substring(2, 5).replace("<", "").trim();
            String docNum   = line1.substring(5, 14);
            char   docCheck = line1.charAt(14);

            result.setDocumentType(new OcrField(docType, 1.0, "MRZ"));
            result.setIssuingCountry(new OcrField(issuer, 1.0, "MRZ"));
            result.setDocumentNumber(new OcrField(docNum.replace("<", "").trim(), 0.99, "MRZ"));

            if (line2.length() >= 30) {
                String dob      = line2.substring(0, 6);
                char   dobCheck = line2.charAt(6);
                String expiry   = line2.substring(8, 14);
                char   expCheck = line2.charAt(14);
                String nat      = line2.substring(15, 18).replace("<", "").trim();

                result.setNationality(new OcrField(nat, 1.0, "MRZ"));
                result.setDateOfBirth(new OcrField(formatDate(dob), 0.99, "MRZ"));
                result.setExpiryDate(new OcrField(formatDate(expiry), 0.99, "MRZ"));

                boolean docNumValid = validateCheckDigit(docNum, docCheck);
                boolean dobValid    = validateCheckDigit(dob, dobCheck);
                boolean expValid    = validateCheckDigit(expiry, expCheck);
                result.setMrzCheckDigits(docNumValid && dobValid && expValid ? "PASS" : "FAIL");
            }
        }

        // Line 3: name
        if (line3.length() >= 30) {
            String nameField = line3;
            String[] parts   = nameField.split("<<");
            String surname   = parts.length > 0 ? parts[0].replace("<", " ").trim() : "";
            String given     = parts.length > 1 ? parts[1].replace("<", " ").trim() : "";
            result.setSurname(new OcrField(surname, 0.99, "MRZ"));
            result.setGivenNames(new OcrField(given, 0.99, "MRZ"));
        }

        result.setSource("MRZ");
        return result;
    }

    // ── ICAO 9303 Check Digit ────────────────────────────────────────────────
    /**
     * Validates ICAO check digit.
     * Characters: A-Z = 10-35, 0-9 = 0-9, < = 0
     * Weighted sum mod 10 with weights [7, 3, 1] repeating.
     */
    public boolean validateCheckDigit(String field, char expectedDigit) {
        if (field == null || field.isBlank()) return false;
        try {
            int sum = 0;
            for (int i = 0; i < field.length(); i++) {
                char c = field.charAt(i);
                int v;
                if (c == '<') v = 0;
                else if (c >= '0' && c <= '9') v = c - '0';
                else if (c >= 'A' && c <= 'Z') v = c - 'A' + 10;
                else return false;
                sum += v * WEIGHTS[i % 3];
            }
            int computed = sum % 10;
            int expected = expectedDigit - '0';
            return computed == expected;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String formatDate(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return yymmdd;
        String yy = yymmdd.substring(0, 2);
        String mm = yymmdd.substring(2, 4);
        String dd = yymmdd.substring(4, 6);
        int year = Integer.parseInt(yy);
        // Assume years 00-30 = 2000s, 31-99 = 1900s
        String century = year <= 30 ? "20" : "19";
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        int m = Integer.parseInt(mm) - 1;
        String monthName = (m >= 0 && m < 12) ? months[m] : mm;
        return dd + " " + monthName + " " + century + yy;
    }

    private String normaliseMrzText(String text) {
        // Normalise line endings and strip spaces OCR inserts within MRZ lines.
        String normalised = text.replace("\r\n", "\n")
                                .replace("\r", "\n")
                                .replace(" ", "");

        // MRZ contains only A-Z, 0-9, and '<'. Any other character is an OCR error.
        // Common misreads of the OCR-B '<' filler character:
        //   lowercase letters (c, e, o …) — visually similar to '<' at low resolution
        //   '«' (double angle quote), '€', '£' — JPEG/font confusion
        // Replacing all lowercase with '<' is safe: MRZ has no lowercase by definition.
        normalised = normalised.replaceAll("[a-z]", "<")
                               .replace("«", "<")
                               .replace("»", "<")
                               .replace("€", "<")
                               .replace("£", "<");

        return normalised;
    }
}
