package com.venus.kyc.screening;

public record ScreeningResult(
        Long resultID,
        Long screeningLogID,
        String contextType, // PEP, ADM, INT, SAN
        String status, // HIT, NO_HIT, IN_PROGRESS
        String alertStatus,
        String alertMessage,
        String alertID) {
}
