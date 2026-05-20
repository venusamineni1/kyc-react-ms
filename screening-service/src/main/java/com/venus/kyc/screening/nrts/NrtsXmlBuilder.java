package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Builds the NRTS submit XML request and computes the SHA-256 checksum.
 *
 * Checksum is computed over the concatenation of:
 *   SrcId + NoR + Type + Name + DOB + Gender + Country + Nationality + CountryResidence
 * Empty/null optional fields are omitted from the concatenation string.
 */
@Component
public class NrtsXmlBuilder {

    /**
     * Builds the full XML body for POST /nrts/submit.
     */
    public String buildSubmitXml(int srcId, ScreeningDTOs.ScreeningInternalRequest req) {
        String name  = formatName(req.firstName(), req.lastName());
        String type  = "I"; // always Individual for this service
        String dob   = req.dateOfBirth()   != null ? req.dateOfBirth()   : "";
        String cntr  = req.citizenship()   != null ? req.citizenship()   : "";
        // Gender, nationality, countryResidence not in current DTO — pass empty
        String gender = "";
        String nat    = cntr; // default nationality = citizenship country
        String cntrRes = cntr;

        String checksum = computeChecksum(srcId, 1, type, name, dob, gender, cntr, nat, cntrRes);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<Request xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "         xsi:schemaLocation=\"http://www.db.com/NLS_NRTS_Definition\"\n" +
               "         xmlns:r=\"http://www.db.com/NLS_NRTS_Request\"\n" +
               "         xmlns:p=\"http://www.db.com/NLS_NRTS_RequestInfo\">\n" +
               "  <r:Meta>\n" +
               "    <p:Version>1.0</p:Version>\n" +
               "    <p:SrcId>" + srcId + "</p:SrcId>\n" +
               "    <p:ChkSum>" + checksum + "</p:ChkSum>\n" +
               "    <p:NoR>1</p:NoR>\n" +
               "  </r:Meta>\n" +
               "  <r:Recs>\n" +
               "    <r:Rec>\n" +
               (req.clientId() != null ? "      <p:ClientId>" + req.clientId() + "</p:ClientId>\n" : "") +
               "      <p:Type>" + type + "</p:Type>\n" +
               "      <p:Name>" + escapeXml(name) + "</p:Name>\n" +
               (!dob.isEmpty()    ? "      <p:DOB>" + dob + "</p:DOB>\n"       : "") +
               (!cntr.isEmpty()   ? "      <p:Cntr>" + cntr + "</p:Cntr>\n"    : "") +
               (!nat.isEmpty()    ? "      <p:Nat>" + nat + "</p:Nat>\n"       : "") +
               (!cntrRes.isEmpty() ? "      <p:CntrRes>" + cntrRes + "</p:CntrRes>\n" : "") +
               "    </r:Rec>\n" +
               "  </r:Recs>\n" +
               "</Request>";
    }

    /**
     * Computes the SHA-256 checksum over the concatenated field string.
     * Format: SrcId + NoR + Type + Name + DOB(yyyyMMdd) + Gender + Country + Nationality + CountryResidence
     * Empty fields are simply omitted (not included in the string).
     */
    public String computeChecksum(int srcId, int noR, String type, String name,
                                   String dob, String gender,
                                   String country, String nationality, String countryResidence) {
        StringBuilder sb = new StringBuilder();
        sb.append(srcId);
        sb.append(noR);
        appendIfNotEmpty(sb, type);
        appendIfNotEmpty(sb, name);
        // DOB: strip dashes for checksum (yyyy-mm-dd → yyyyMMdd)
        if (dob != null && !dob.isBlank()) {
            appendIfNotEmpty(sb, dob.replace("-", ""));
        }
        appendIfNotEmpty(sb, gender);
        appendIfNotEmpty(sb, country);
        appendIfNotEmpty(sb, nationality);
        appendIfNotEmpty(sb, countryResidence);

        return sha256Hex(sb.toString());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void appendIfNotEmpty(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value);
        }
    }

    private String formatName(String firstName, String lastName) {
        if (lastName != null && !lastName.isBlank() && firstName != null && !firstName.isBlank()) {
            return lastName + "," + firstName;
        }
        return (firstName != null ? firstName : "") + (lastName != null ? lastName : "");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
