package com.venus.kyc.viewer.screening;

import java.time.LocalDateTime;

public record ScreeningLog(
        Long logID,
        Long clientID,
        String requestPayload,
        String responsePayload,
        String overallStatus,
        String externalRequestID,
        LocalDateTime createdAt) {
}
