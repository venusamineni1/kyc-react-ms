package com.venus.kyc.screening.nrts;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Thin HTTP wrapper for all 4 NRTS API endpoints.
 * Authentication: Basic Auth (Base64 username:password) over HTTPS.
 * All methods throw NrtsApiException on non-2xx responses.
 */
@Component
public class NrtsHttpClient {

    private final RestClient restClient;
    private final NrtsConfig config;

    public NrtsHttpClient(NrtsConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader(config.username(), config.password()))
                .build();
    }

    // ── Endpoint 1: POST /nrts/submit ─────────────────────────────────────────

    /**
     * Submits one client for screening.
     *
     * @param xmlBody the complete XML request body
     * @return raw XML response body (200 or 202)
     */
    public NrtsRawResponse submit(String xmlBody) {
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/nrts/submit")
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_XML)
                    .body(xmlBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new NrtsApiException(res.getStatusCode().value(),
                                "NRTS submit failed: HTTP " + res.getStatusCode().value());
                    })
                    .toEntity(String.class);

            return new NrtsRawResponse(response.getStatusCode().value(), response.getBody());
        } catch (NrtsApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NrtsApiException(0, "NRTS submit HTTP error: " + e.getMessage());
        }
    }

    // ── Endpoint 2: GET /nrts/get_status/:processId ───────────────────────────

    /**
     * Gets the current investigation status for a process.
     *
     * @param processId the NRTS process ID returned by submit
     * @return raw XML response body
     */
    public NrtsRawResponse getStatus(long processId) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/nrts/get_status/{processId}", processId)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new NrtsApiException(res.getStatusCode().value(),
                                "NRTS get_status failed: HTTP " + res.getStatusCode().value());
                    })
                    .toEntity(String.class);

            return new NrtsRawResponse(response.getStatusCode().value(), response.getBody());
        } catch (NrtsApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NrtsApiException(0, "NRTS get_status HTTP error: " + e.getMessage());
        }
    }

    // ── Endpoint 3: GET /nrts/get_final_request_details/:requestId ────────────

    /**
     * Retrieves alert decision history for a finalized client.
     *
     * @param requestId the NRTS ReqId for the client
     * @return raw JSON response body
     */
    public NrtsRawResponse getDetails(long requestId) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/nrts/get_final_request_details/{requestId}", requestId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new NrtsApiException(res.getStatusCode().value(),
                                "NRTS get_details failed: HTTP " + res.getStatusCode().value());
                    })
                    .toEntity(String.class);

            return new NrtsRawResponse(response.getStatusCode().value(), response.getBody());
        } catch (NrtsApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NrtsApiException(0, "NRTS get_details HTTP error: " + e.getMessage());
        }
    }

    // ── Endpoint 4: GET /nrts/get_document/:documentId ────────────────────────

    /**
     * Downloads a Filenet document by its ID.
     *
     * @param documentId the filenet-id (e.g. "{80E07D71-0000-CD1F-AA23-B5BAA86F4FF6}")
     * @return full ResponseEntity with byte[] body and Content-Type / Content-Disposition headers
     */
    public ResponseEntity<byte[]> getDocument(String documentId) {
        try {
            return restClient.get()
                    .uri("/nrts/get_document/{documentId}", documentId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new NrtsApiException(res.getStatusCode().value(),
                                "NRTS get_document failed: HTTP " + res.getStatusCode().value());
                    })
                    .toEntity(byte[].class);
        } catch (NrtsApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NrtsApiException(0, "NRTS get_document HTTP error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String basicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public record NrtsRawResponse(int httpStatus, String body) {}

    public static class NrtsApiException extends RuntimeException {
        private final int httpStatus;

        public NrtsApiException(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() { return httpStatus; }
    }
}
