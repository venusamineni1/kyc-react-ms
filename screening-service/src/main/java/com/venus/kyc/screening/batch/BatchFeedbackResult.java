package com.venus.kyc.screening.batch;

public record BatchFeedbackResult(
        Long resultID,
        Long batchID,
        String recordID,
        String matchID,
        String matchName,
        String matchScore,
        String status) {
}
