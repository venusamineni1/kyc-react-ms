package com.venus.kyc.viewer;

import java.time.LocalDate;

public record Client(
        Long clientID,
        String titlePrefix,
        String firstName,
        String middleName,
        String lastName,
        String titleSuffix,
        String citizenship1,
        String citizenship2,
        LocalDate onboardingDate,
        String status,
        String nameAtBirth,
        String nickName,
        String gender,
        LocalDate dateOfBirth,
        String language,
        String occupation,
        String countryOfTax,
        String sourceOfFundsCountry,
        String fatcaStatus,
        String crsStatus,
        String placeOfBirth,
        String cityOfBirth,
        String countryOfBirth,
        java.util.List<Address> addresses,
        java.util.List<Identifier> identifiers,
        java.util.List<RelatedParty> relatedParties,
        java.util.List<Account> accounts,
        java.util.List<Portfolio> portfolios) {
}
