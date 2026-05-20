package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses XML responses from:
 *  - POST /nrts/submit  → NrtsSubmitResult
 *  - GET  /nrts/get_status/:processId → NrtsStatusResult
 */
@Component
public class NrtsXmlParser {

    // ── Submit response ───────────────────────────────────────────────────────

    public record NrtsSubmitResult(
            boolean anyAlerts,   // true → 202, false → 200
            Long processId,
            String stat,
            String errorMessage
    ) {}

    /**
     * Parses the XML body of POST /nrts/submit responses (200 and 202).
     */
    public NrtsSubmitResult parseSubmitResponse(String xml) {
        try {
            Document doc = parse(xml);
            String stat       = getText(doc, "Stat");
            String anyAlerts  = getText(doc, "AnyAlerts");
            String processId  = getText(doc, "ProcessId");
            String msg        = getText(doc, "Msg");

            return new NrtsSubmitResult(
                    "T".equalsIgnoreCase(anyAlerts),
                    processId != null && !processId.isBlank() ? Long.parseLong(processId.trim()) : null,
                    stat,
                    msg
            );
        } catch (Exception e) {
            throw new NrtsParseException("Failed to parse submit response: " + e.getMessage(), e);
        }
    }

    // ── Status response ───────────────────────────────────────────────────────

    public record NrtsStatusResult(
            Long processId,
            String overallStat,       // "In progress" | "With SIU" | "Finished"
            int noR,
            List<NrtsClientResult> clients
    ) {
        public boolean isFinalized() {
            return "FINISHED".equalsIgnoreCase(overallStat)
                    || clients.stream().allMatch(NrtsClientResult::finalFlag);
        }
    }

    public record NrtsClientResult(
            Long reqId,
            String clientId,
            String type,
            String name,
            boolean finalFlag,
            List<ScreeningDTOs.AlertContext> alerts
    ) {}

    /**
     * Parses the XML body of GET /nrts/get_status/:processId (200 OK).
     */
    public NrtsStatusResult parseStatusResponse(String xml) {
        try {
            Document doc = parse(xml);

            String stat      = getText(doc, "Stat");
            String procId    = getText(doc, "ProcId");
            String norStr    = getText(doc, "NoR");
            int noR = (norStr != null && !norStr.isBlank()) ? Integer.parseInt(norStr.trim()) : 0;

            List<NrtsClientResult> clients = new ArrayList<>();
            NodeList resultNodes = doc.getElementsByTagNameNS("*", "Result");
            for (int i = 0; i < resultNodes.getLength(); i++) {
                Element result = (Element) resultNodes.item(i);

                // <r:Rec> child
                String reqId    = getChildText(result, "ReqId");
                String clientId = getChildText(result, "ClientId");
                String type     = getChildText(result, "Type");
                String name     = getChildText(result, "Name");
                String finalStr = getChildText(result, "Final");

                // <r:Alerts> → <r:Alert> children
                List<ScreeningDTOs.AlertContext> alerts = new ArrayList<>();
                NodeList alertNodes = result.getElementsByTagNameNS("*", "Alert");
                for (int j = 0; j < alertNodes.getLength(); j++) {
                    Element alert = (Element) alertNodes.item(j);
                    String context  = getChildText(alert, "Context");
                    String status   = getChildText(alert, "Status");
                    String statusId = getChildText(alert, "StatusId");
                    alerts.add(new ScreeningDTOs.AlertContext(context, status, statusId));
                }

                clients.add(new NrtsClientResult(
                        reqId != null && !reqId.isBlank() ? Long.parseLong(reqId.trim()) : null,
                        clientId, type, name,
                        "T".equalsIgnoreCase(finalStr != null ? finalStr.trim() : ""),
                        alerts
                ));
            }

            return new NrtsStatusResult(
                    procId != null && !procId.isBlank() ? Long.parseLong(procId.trim()) : null,
                    stat,
                    noR,
                    clients
            );
        } catch (NrtsParseException e) {
            throw e;
        } catch (Exception e) {
            throw new NrtsParseException("Failed to parse status response: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Disable external entity processing (security)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Gets text content of the first element matching localName (any namespace). */
    private String getText(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    /** Gets text content of the first child element matching localName within a parent. */
    private String getChildText(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    public static class NrtsParseException extends RuntimeException {
        public NrtsParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
