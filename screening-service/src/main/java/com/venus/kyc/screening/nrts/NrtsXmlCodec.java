package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * XML implementation of NrtsPayloadCodec.
 *
 * Active by default (nrts.format=xml or property absent).
 * Switch to NrtsJsonCodec by setting nrts.format=json when NRTS migrates.
 *
 * XML is built via DOM so all field values are escaped automatically by the
 * serializer — no manual escaping or string concatenation.
 *
 * Checksum: SHA-256 over SrcId + NoR + (per record) Type + Name + DOB(yyyyMMdd)
 *           + Gender + Country + Nationality + CountryResidence.
 *           Empty / null fields are omitted from the concatenation per the spec.
 */
@Component
@ConditionalOnProperty(name = "nrts.format", havingValue = "xml", matchIfMissing = true)
public class NrtsXmlCodec implements NrtsPayloadCodec {

    private static final String NS_REQ  = "http://www.db.com/NLS_NRTS_Request";
    private static final String NS_INFO = "http://www.db.com/NLS_NRTS_RequestInfo";
    private static final String NS_XSI  = "http://www.w3.org/2001/XMLSchema-instance";

    // ── Serialize ─────────────────────────────────────────────────────────────

    @Override
    public String serializeSubmit(int srcId, List<NrtsRecord> records) {
        try {
            String checksum = computeChecksum(srcId, records);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().newDocument();

            Element root = doc.createElementNS(NS_REQ, "r:Request");
            root.setAttribute("xmlns:xsi", NS_XSI);
            root.setAttribute("xmlns:r", NS_REQ);
            root.setAttribute("xmlns:p", NS_INFO);
            root.setAttributeNS(NS_XSI, "xsi:schemaLocation", "http://www.db.com/NLS_NRTS_Definition");
            doc.appendChild(root);

            // Meta
            Element meta = r(doc, "Meta");
            root.appendChild(meta);
            meta.appendChild(p(doc, "Version", "1.0"));
            meta.appendChild(p(doc, "SrcId", String.valueOf(srcId)));
            meta.appendChild(p(doc, "ChkSum", checksum));
            meta.appendChild(p(doc, "NoR", String.valueOf(records.size())));

            // Recs
            Element recs = r(doc, "Recs");
            root.appendChild(recs);
            for (NrtsRecord rec : records) {
                recs.appendChild(buildRec(doc, rec));
            }

            return toXmlString(doc);
        } catch (Exception e) {
            throw new CodecException("Failed to serialize NRTS submit XML: " + e.getMessage(), e);
        }
    }

    private Element buildRec(Document doc, NrtsRecord rec) {
        Element recEl = r(doc, "Rec");
        pIf(doc, recEl, "ClientId",  rec.clientId());
        recEl.appendChild(p(doc, "Type", rec.type()));
        // XML spec requires lastName,firstName format
        recEl.appendChild(p(doc, "Name", xmlName(rec)));
        pIf(doc, recEl, "DOB",       rec.dateOfBirth());
        pIf(doc, recEl, "G",         rec.gender());
        pIf(doc, recEl, "IdType",    rec.idType());
        pIf(doc, recEl, "IdNr",      rec.idNumber());
        pIf(doc, recEl, "Risk",      rec.riskRating());
        pIf(doc, recEl, "Comment",   rec.comment());
        pIf(doc, recEl, "Cntr",      rec.country());
        pIf(doc, recEl, "Nat",       rec.nationality());
        pIf(doc, recEl, "CntrRes",   rec.countryOfResidence());
        pIf(doc, recEl, "Prov",      rec.province());
        return recEl;
    }

    /** XML name: lastName,firstName (per spec). */
    private String xmlName(NrtsRecord rec) {
        String last  = rec.lastName()  != null ? rec.lastName()  : "";
        String first = rec.firstName() != null ? rec.firstName() : "";
        if (!last.isBlank() && !first.isBlank()) return last + "," + first;
        return last.isBlank() ? first : last;
    }

    /** Checksum name: firstName lastName (space-separated, per verified spec example). */
    private String checksumName(NrtsRecord rec) {
        String first = rec.firstName() != null ? rec.firstName() : "";
        String last  = rec.lastName()  != null ? rec.lastName()  : "";
        if (!first.isBlank() && !last.isBlank()) return first + " " + last;
        return first.isBlank() ? last : first;
    }

    /** Creates an r:localName element (NLS_NRTS_Request namespace). */
    private Element r(Document doc, String localName) {
        return doc.createElementNS(NS_REQ, "r:" + localName);
    }

    /** Creates a p:localName element with text content (NLS_NRTS_RequestInfo namespace). */
    private Element p(Document doc, String localName, String value) {
        Element e = doc.createElementNS(NS_INFO, "p:" + localName);
        e.setTextContent(value != null ? value : "");
        return e;
    }

    /** Appends p:localName only if value is non-blank. */
    private void pIf(Document doc, Element parent, String localName, String value) {
        if (value != null && !value.isBlank()) {
            parent.appendChild(p(doc, localName, value));
        }
    }

    private String toXmlString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    // ── Checksum ──────────────────────────────────────────────────────────────
    //
    // Per spec: SrcId + NoR + (per record) Type + Name + DOB(yyyyMMdd) + Gender
    //           + Country + Nationality + CountryResidence
    // Empty/null fields are simply omitted from the concatenation string.
    // Name format in checksum is "firstName lastName" (verified against spec example:
    // SHA-256("22341IJohn Doe19750504MUKUKUK") == d891a8780c99120ded2885a2ff665bac0d00c14049c1ff36f966662ea33373cf)

    String computeChecksum(int srcId, List<NrtsRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append(srcId);
        sb.append(records.size());
        for (NrtsRecord rec : records) {
            appendNonBlank(sb, rec.type());
            appendNonBlank(sb, checksumName(rec));
            if (rec.dateOfBirth() != null && !rec.dateOfBirth().isBlank()) {
                // Strip dashes: yyyy-MM-dd → yyyyMMdd
                sb.append(rec.dateOfBirth().replace("-", ""));
            }
            appendNonBlank(sb, rec.gender());
            appendNonBlank(sb, rec.country());
            appendNonBlank(sb, rec.nationality());
            appendNonBlank(sb, rec.countryOfResidence());
        }
        return sha256Hex(sb.toString());
    }

    private void appendNonBlank(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) sb.append(value);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ── Parse: Submit response ────────────────────────────────────────────────

    @Override
    public SubmitResult parseSubmitResponse(String xml) {
        try {
            Document doc = parseXml(xml);
            String stat      = text(doc, "Stat");
            String anyAlerts = text(doc, "AnyAlerts");
            String processId = text(doc, "ProcessId");
            String msg       = text(doc, "Msg");
            return new SubmitResult(
                    "T".equalsIgnoreCase(anyAlerts),
                    toLong(processId),
                    stat,
                    msg
            );
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("Failed to parse NRTS submit response: " + e.getMessage(), e);
        }
    }

    // ── Parse: Status response ────────────────────────────────────────────────

    @Override
    public StatusResult parseStatusResponse(String xml) {
        try {
            Document doc = parseXml(xml);

            String stat   = text(doc, "Stat");
            String procId = text(doc, "ProcId");
            String norStr = text(doc, "NoR");
            int noR = (norStr != null && !norStr.isBlank()) ? Integer.parseInt(norStr.trim()) : 0;

            List<ClientResult> clients = new ArrayList<>();
            NodeList resultNodes = doc.getElementsByTagNameNS("*", "Result");
            for (int i = 0; i < resultNodes.getLength(); i++) {
                Element result = (Element) resultNodes.item(i);

                String reqId    = childText(result, "ReqId");
                String clientId = childText(result, "ClientId");
                String type     = childText(result, "Type");
                String name     = childText(result, "Name");
                String finalStr = childText(result, "Final");

                List<ScreeningDTOs.AlertContext> alerts = new ArrayList<>();
                NodeList alertNodes = result.getElementsByTagNameNS("*", "Alert");
                for (int j = 0; j < alertNodes.getLength(); j++) {
                    Element alert = (Element) alertNodes.item(j);
                    alerts.add(new ScreeningDTOs.AlertContext(
                            childText(alert, "Context"),
                            childText(alert, "Status"),
                            childText(alert, "StatusId")
                    ));
                }

                clients.add(new ClientResult(
                        toLong(reqId),
                        clientId, type, name,
                        "T".equalsIgnoreCase(finalStr != null ? finalStr.trim() : ""),
                        alerts
                ));
            }

            return new StatusResult(toLong(procId), stat, noR, clients);
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("Failed to parse NRTS status response: " + e.getMessage(), e);
        }
    }

    // ── XML parse helpers ─────────────────────────────────────────────────────

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String text(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }

    private String childText(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }

    private Long toLong(String s) {
        return (s != null && !s.isBlank()) ? Long.parseLong(s.trim()) : null;
    }
}
