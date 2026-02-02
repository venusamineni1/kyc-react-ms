package com.venus.kyc.screening;

import java.util.List;

public class ScreeningDTOs {

    public record ScreeningInternalRequest(
            Long clientId,
            String firstName,
            String lastName,
            String dateOfBirth,
            String citizenship) {
    }

    public record InitiateScreeningResponse(
            boolean hit,
            String requestId) {
    }

    public record ScreeningStatusResponse(
            String requestId,
            List<ContextResult> results) {
    }

    public record ContextResult(
            String contextType,
            String status,
            String alertMessage) {
    }

    // Mock External API DTOs
    public record ExternalScreeningRequest(
            String name,
            String dob,
            String country) {
    }

    public record ExternalScreeningResponse(
            String requestId,
            String status) {
    }
}
