package com.venus.kyc.screening.nrts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venus.kyc.screening.ScreeningDTOs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the JSON response from GET /nrts/get_final_request_details/:requestId.
 */
@Component
public class NrtsJsonParser {

    private final ObjectMapper objectMapper;

    public NrtsJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses the details JSON into an AlertDetailsResponse.
     */
    public ScreeningDTOs.AlertDetailsResponse parseDetailsResponse(long requestId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String status = root.path("status").asText();

            List<ScreeningDTOs.AlertEntry> alerts = new ArrayList<>();
            JsonNode historyArray = root.path("alerthistory");

            if (historyArray.isArray()) {
                for (JsonNode alertNode : historyArray) {
                    String alertId         = alertNode.path("alert-id").asText(null);
                    String alertStatus     = alertNode.path("alert-status").asText(null);
                    String lastDecision    = alertNode.path("last-decision-date").asText(null);
                    String lastOperator    = alertNode.path("last-operator").asText(null);
                    String lastComments    = alertNode.path("last-comments").asText(null);
                    String accountId       = alertNode.path("account-id").asText(null);

                    // Derive context from alert-id (e.g. "2180_INT!WR_..." → "INT")
                    String context = deriveContext(alertId);

                    // Parse hits
                    List<ScreeningDTOs.HitEntry> hits = new ArrayList<>();
                    JsonNode hitsNode = alertNode.path("hits");
                    if (hitsNode.isArray()) {
                        for (JsonNode hit : hitsNode) {
                            hits.add(new ScreeningDTOs.HitEntry(
                                    hit.path("country").asText(null),
                                    hit.path("city").asText(null),
                                    hit.path("name").asText(null),
                                    hit.path("origin").asText(null),
                                    hit.path("keywords").asText(null),
                                    hit.path("type").asText(null)
                            ));
                        }
                    }

                    // Parse decision history
                    List<ScreeningDTOs.DecisionEntry> decisions = new ArrayList<>();
                    JsonNode decisionArray = alertNode.path("decision-history");
                    if (decisionArray.isArray()) {
                        for (JsonNode dec : decisionArray) {
                            ScreeningDTOs.DocumentRef docRef = null;
                            JsonNode docNode = dec.path("document");
                            if (!docNode.isNull() && docNode.isObject()) {
                                docRef = new ScreeningDTOs.DocumentRef(
                                        docNode.path("filenet-id").asText(null),
                                        docNode.path("comments").asText(null),
                                        null
                                );
                            }
                            decisions.add(new ScreeningDTOs.DecisionEntry(
                                    dec.path("date").asText(null),
                                    dec.path("operator").asText(null),
                                    dec.path("state").asText(null),
                                    dec.path("comments").asText(null),
                                    docRef
                            ));
                        }
                    }

                    // Parse alert-level documents
                    List<ScreeningDTOs.DocumentRef> alertDocs = new ArrayList<>();
                    JsonNode alertDocsNode = alertNode.path("alert-documents");
                    if (alertDocsNode.isArray()) {
                        for (JsonNode d : alertDocsNode) {
                            alertDocs.add(new ScreeningDTOs.DocumentRef(
                                    d.path("filenet-id").asText(null),
                                    d.path("comments").asText(null),
                                    d.path("operator").asText(null)
                            ));
                        }
                    }

                    alerts.add(new ScreeningDTOs.AlertEntry(
                            alertId, context, alertStatus,
                            lastDecision, lastOperator, lastComments,
                            hits.isEmpty() ? null : hits,
                            decisions,
                            alertDocs.isEmpty() ? null : alertDocs
                    ));
                }
            }

            return new ScreeningDTOs.AlertDetailsResponse(requestId, status, alerts);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse NRTS details response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts context from alert-id format: "{index}_{CONTEXT}!{accountId}"
     * e.g. "2180_INT!WR_948383" → "INT"
     */
    private String deriveContext(String alertId) {
        if (alertId == null) return null;
        try {
            int underscore = alertId.indexOf('_');
            int exclamation = alertId.indexOf('!');
            if (underscore >= 0 && exclamation > underscore) {
                return alertId.substring(underscore + 1, exclamation);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
