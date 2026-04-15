package com.venus.kyc.orchestration.service;

import com.venus.kyc.orchestration.dto.KycStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fires a webhook POST to the caller-supplied URL when an orchestration status is finalized.
 * All calls are async and non-blocking. Retries up to 3 times with exponential back-off.
 * On exhaustion the event is dead-lettered to the error log for ops alerting (KYC-NF-06).
 */
@Service
@Slf4j
public class WebhookNotificationService {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final RestTemplate externalRestTemplate;
    private final Executor kycOrchestrationExecutor;

    public WebhookNotificationService(
            @Qualifier("externalRestTemplate") RestTemplate externalRestTemplate,
            @Qualifier("kycOrchestrationExecutor") Executor kycOrchestrationExecutor) {
        this.externalRestTemplate = externalRestTemplate;
        this.kycOrchestrationExecutor = kycOrchestrationExecutor;
    }

    /**
     * Asynchronously POSTs the status payload to webhookUrl.
     * Returns immediately; retries happen on the kycOrchestrationExecutor thread pool.
     */
    public void notify(String webhookUrl, KycStatusResponse payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        CompletableFuture.runAsync(() -> postWithRetry(webhookUrl, payload), kycOrchestrationExecutor);
    }

    private void postWithRetry(String webhookUrl, KycStatusResponse payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-KYC-Id", payload.getKycId());
        HttpEntity<KycStatusResponse> request = new HttpEntity<>(payload, headers);

        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                externalRestTemplate.postForEntity(webhookUrl, request, Void.class);
                log.info("Webhook delivered: kycId={} url={} attempt={}",
                        payload.getKycId(), webhookUrl, attempt);
                return;
            } catch (Exception ex) {
                log.warn("Webhook attempt {}/{} failed for kycId={} url={}: {}",
                        attempt, MAX_RETRIES, payload.getKycId(), webhookUrl, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep(backoffMs);
                    backoffMs *= 2;
                }
            }
        }
        // Dead-letter: all retries exhausted
        log.error("DEAD-LETTER: Webhook delivery failed after {} attempts. kycId={} url={}",
                MAX_RETRIES, payload.getKycId(), webhookUrl);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
