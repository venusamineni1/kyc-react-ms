package com.venus.kyc.screening.nrts;

/**
 * Format-agnostic model for one client screening record.
 * Serves as the stable contract between the business layer and the
 * serialization layer (XML today, JSON when NRTS migrates).
 *
 * Field names map 1-to-1 with the NRTS spec XML elements.
 * Null fields are omitted from the serialized payload.
 */
public record NrtsRecord(
        String clientId,          // <p:ClientId>  optional, ≤100 alphanumeric chars
        String type,              // <p:Type>       "I" (Individual) or "C" (Company)
        String firstName,         // first part of <p:Name> — XML serializes as lastName,firstName; checksum uses firstName lastName
        String lastName,          // second part of <p:Name>
        String dateOfBirth,       // <p:DOB>        yyyy or yyyy-MM-dd
        String gender,            // <p:G>          "M" or "F" — only for individuals
        String country,           // <p:Cntr>       ISO alpha-2
        String nationality,       // <p:Nat>        ISO alpha-2 — only for individuals
        String countryOfResidence,// <p:CntrRes>    ISO alpha-2
        String idType,            // <p:IdType>     alphanumeric
        String idNumber,          // <p:IdNr>       alphanumeric
        String riskRating,        // <p:Risk>       "H", "M", or "L"
        String comment,           // <p:Comment>    alphanumeric free text
        String province           // <p:Prov>       alphanumeric
) {}
