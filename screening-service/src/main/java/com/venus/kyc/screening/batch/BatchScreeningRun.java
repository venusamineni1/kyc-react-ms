package com.venus.kyc.screening.batch;

import java.time.LocalDateTime;

/**
 * Tracks a single mass-screening run (e.g. 700K clients) across all sub-batches.
 * One row = one CSV file drop → N BatchRun records linked via runGroupId.
 *
 * Overall status lifecycle:
 *   DETECTED → INGESTING → DISPATCHING → COMPLETED
 *                                       → FAILED
 */
public record BatchScreeningRun(
        Long id,
        String runGroupId,
        String fileName,
        String systemId,
        Long totalClientCount,
        Integer totalBatches,
        Integer batchesCompleted,
        String overallStatus,
        Boolean persistClients,
        String callbackWebhookUrl,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
