package com.venus.kyc.screening.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fires an async HTTP webhook to the downstream system when all sub-batches of a
 * mass-screening run have been dispatched/uploaded.
 *
 * <p>Retry policy: up to 3 attempts with exponential back-off (2s → 4s → 8s).
 */
@Service
public class BatchCompletionNotifier {

    private static final Logger log = LoggerFactory.getLogger(BatchCompletionNotifier.class);
    private static final int MAX_RETRIES = 3;

    private final RestClient restClient;
    private final BatchScreeningRunRepository runRepository;

    public BatchCompletionNotifier(RestClient.Builder restClientBuilder,
                                   BatchScreeningRunRepository runRepository) {
        this.restClient = restClientBuilder.build();
        this.runRepository = runRepository;
    }

    /**
     * Called after all sub-batches for {@code runGroupId} have been dispatched.
     * Marks the overall run COMPLETED and fires the callback webhook if configured.
     */
    @Async
    public void notifyComplete(String runGroupId, int totalBatches, long totalClients) {
        runRepository.updateStatus(runGroupId, "COMPLETED");
        log.info("[{}] All {} batches dispatched ({} clients total). Run marked COMPLETED.",
                runGroupId, totalBatches, totalClients);

        runRepository.findByRunGroupId(runGroupId).ifPresent(run -> {
            String webhookUrl = run.callbackWebhookUrl();
            if (webhookUrl == null || webhookUrl.isBlank()) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "MASS_SCREENING_BATCH_COMPLETE");
            payload.put("runGroupId", runGroupId);
            payload.put("systemId", run.systemId());
            payload.put("fileName", run.fileName());
            payload.put("totalClientsProcessed", totalClients);
            payload.put("totalBatches", totalBatches);
            payload.put("completedAt", LocalDateTime.now().toString());

            postWithRetry(webhookUrl, payload, runGroupId);
        });
    }

    private void postWithRetry(String webhookUrl, Map<String, Object> payload, String runGroupId) {
        int attempt = 0;
        long delayMs = 2000;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                log.info("[{}] Webhook delivered to {} (attempt {})", runGroupId, webhookUrl, attempt);
                return;
            } catch (Exception e) {
                log.warn("[{}] Webhook attempt {}/{} failed for {}: {}", runGroupId, attempt, MAX_RETRIES, webhookUrl, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                    delayMs *= 2;
                }
            }
        }
        log.error("[{}] Webhook delivery failed after {} attempts for {}", runGroupId, MAX_RETRIES, webhookUrl);
    }
}
