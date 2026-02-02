package com.venus.kyc.screening.batch;

import java.time.LocalDateTime;

public record BatchRun(
        Long batchID,
        String batchName,
        String status,
        String notificationStatus,
        Integer feedbackCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
