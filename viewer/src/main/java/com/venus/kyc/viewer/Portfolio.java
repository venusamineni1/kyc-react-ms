package com.venus.kyc.viewer;

import java.time.LocalDate;

public record Portfolio(
                Long portfolioID,
                Long clientID,
                String accountNumber,
                String portfolioText,
                LocalDate onboardingDate,
                LocalDate offboardingDate,
                String status) {
}
