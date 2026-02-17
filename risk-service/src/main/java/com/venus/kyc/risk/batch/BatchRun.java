package com.venus.kyc.risk.batch;

import java.time.LocalDateTime;

public record BatchRun(
        Long batchID,
        String batchName,
        String runStatus,
        String notificationStatus,
        Integer feedbackCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
