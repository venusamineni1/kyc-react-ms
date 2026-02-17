package com.venus.kyc.risk.batch.model;

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
                String addressLine1,
                String city,
                String zipCode,
                String province,
                String country,
                String nationality,
                String legDocType,
                String idNumber,
                String placeOfBirth,
                String cityOfBirth,
                String countryOfBirth) {
}
