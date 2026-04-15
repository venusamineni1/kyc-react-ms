package com.venus.kyc.orchestration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Standard error response returned by all endpoints when a non-2xx status occurs.")
public class ErrorResponse {

    @Schema(description = "Timestamp of when the error occurred (ISO-8601).",
            example = "2026-04-11T10:05:00.000")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code.",
            example = "400")
    private int status;

    @Schema(description = "Short error category.",
            example = "Validation Failed")
    private String error;

    @Schema(description = "Human-readable explanation of what went wrong.",
            example = "businessLine: businessLine must be EIS or PPR")
    private String message;
}
