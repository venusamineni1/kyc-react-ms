package com.venus.kyc.viewer;

import java.time.LocalDate;
import java.util.List;

public record RelatedParty(
                Long relatedPartyID,
                Long clientID,
                String relationType,
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
                List<Address> addresses,
                List<Identifier> identifiers) {
}
