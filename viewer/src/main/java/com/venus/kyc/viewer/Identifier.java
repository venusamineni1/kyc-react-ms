package com.venus.kyc.viewer;

public record Identifier(
                Long identifierID,
                String identifierType,
                String identifierValue,
                String issuingAuthority,
                String identifierNumber) {
}
