package com.venus.kyc.screening;

import java.util.List;

public interface ScreeningProvider {
    /**
     * Initiates a screening request with an external provider.
     * 
     * @param request The abstract screening request.
     * @return The external request ID.
     */
    String initiate(ScreeningDTOs.ExternalScreeningRequest request);

    /**
     * Checks the status of a specific request.
     * 
     * @param externalRequestId The external request ID.
     * @return The updated results, or empty if still pending/unchanged.
     */
    List<ScreeningDTOs.ContextResult> checkStatus(String externalRequestId);
}
