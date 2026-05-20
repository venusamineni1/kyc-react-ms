package com.venus.kyc.screening.batch;

import java.time.LocalDateTime;

public record BatchRun(
                Long batchID,
                String batchName,
                String runStatus,
                String notificationStatus,
                Integer feedbackCount,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                Long mappingSnapshotID,
                Integer clientCount,
                String runGroupId) {

        // Backwards-compatible constructor for existing code (no runGroupId)
        public BatchRun(Long batchID, String batchName, String runStatus, String notificationStatus,
                        Integer feedbackCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
                this(batchID, batchName, runStatus, notificationStatus, feedbackCount, createdAt, updatedAt, null, null, null);
        }

        // Constructor without runGroupId (snapshot + count, no group)
        public BatchRun(Long batchID, String batchName, String runStatus, String notificationStatus,
                        Integer feedbackCount, LocalDateTime createdAt, LocalDateTime updatedAt,
                        Long mappingSnapshotID, Integer clientCount) {
                this(batchID, batchName, runStatus, notificationStatus, feedbackCount, createdAt, updatedAt, mappingSnapshotID, clientCount, null);
        }
}
