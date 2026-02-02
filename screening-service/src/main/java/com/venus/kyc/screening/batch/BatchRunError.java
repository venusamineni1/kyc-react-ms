package com.venus.kyc.screening.batch;

public record BatchRunError(
        Long errorID,
        Long batchID,
        String recordID,
        String errorCode,
        String errorMessage) {
}
