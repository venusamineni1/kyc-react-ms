package com.venus.kyc.risk;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Mock implementation of RiskProvider.
 * Replaces the old dummy-external-api REST endpoint — now a plain Java call,
 * no HTTP round-trip, no self-calling anti-pattern.
 *
 * Active when crre.mock=true (default for local dev and CI).
 *
 * Deterministic hit logic:
 *   - clientAdoptionCountry = "CU" (Cuba) → HIGH risk
 *   - Everything else → LOW risk
 */
@Component
@ConditionalOnProperty(name = "crre.mock", havingValue = "true", matchIfMissing = true)
public class MockRiskProvider implements RiskProvider {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    @Override
    public RiskDTOs.CalculateRiskResponse calculateRisk(RiskDTOs.CalculateRiskRequest request) {
        String recordId = extractRecordId(request);
        boolean isHighRisk = isHighRiskCountry(request);

        String riskLevel = isHighRisk ? "HIGH" : "LOW";
        int overallScore = isHighRisk ? 9 : 1;

        // Pillar scores proportional to overall risk (meaningful radar chart)
        int pEntity   = isHighRisk ? 82 : 14;
        int pIndustry = isHighRisk ? 75 : 18;
        int pGeo      = isHighRisk ? 91 : 12;
        int pProduct  = isHighRisk ? 70 : 10;
        int pChannel  = isHighRisk ? 85 :  8;

        String now = OffsetDateTime.now().format(TS_FMT);

        RiskDTOs.Header responseHeader = new RiskDTOs.Header(
                request.header() != null ? request.header().callerSystem() : "KYC-SERVICE",
                null,
                "2.0",
                UUID.randomUUID().toString(),
                null,
                now,
                "114654-1",
                "CRRE 22.2",
                "122"
        );

        RiskDTOs.ProcessStatus processStatus = new RiskDTOs.ProcessStatus("Success", 1, 0, 0);

        RiskDTOs.OverallRiskAssessment overall = new RiskDTOs.OverallRiskAssessment(
                null, overallScore, riskLevel, riskLevel, riskLevel, "Standard", "");

        RiskDTOs.EntityRiskType entity = new RiskDTOs.EntityRiskType(
                null, pEntity, riskLevel, "",
                List.of(new RiskDTOs.RiskClassification(
                        "typeKYCLegalEntityCode", "NP4", pEntity, null, null, "N")));

        RiskDTOs.IndustryRiskType industry = new RiskDTOs.IndustryRiskType(
                null, pIndustry, riskLevel, null,
                List.of(new RiskDTOs.RiskClassification(
                        "occupationCode", "00101", pIndustry, null, null, "N")));

        RiskDTOs.GeoRiskType geo = new RiskDTOs.GeoRiskType(
                null, null, null, pGeo, riskLevel, null,
                List.of(
                        new RiskDTOs.RiskClassification(
                                "countryOfNationality", "DE", pGeo, null, null, "N"),
                        new RiskDTOs.RiskClassification(
                                "originOfFunds", "DE", pGeo, null, null, "N"),
                        new RiskDTOs.RiskClassification(
                                "clientDomicile", "DE", pGeo, null, null, "N")));

        RiskDTOs.ProductRiskType product = new RiskDTOs.ProductRiskType(
                null, pProduct, riskLevel, null,
                List.of(new RiskDTOs.RiskClassification(
                        "productCode", "OAP1", pProduct, null, null, "N")));

        RiskDTOs.ChannelRiskType channel = new RiskDTOs.ChannelRiskType(
                null, pChannel, riskLevel, null,
                List.of(new RiskDTOs.RiskClassification(
                        "channelCode", "CHN05", pChannel, null, null, "N")));

        RiskDTOs.ClientRiskRatingResponseItem responseItem =
                new RiskDTOs.ClientRiskRatingResponseItem(
                        recordId, null,
                        extractAdoptionCountry(request),
                        null,
                        overall, entity, industry, geo, product, channel);

        return new RiskDTOs.CalculateRiskResponse(responseHeader, processStatus, List.of(responseItem));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isHighRiskCountry(RiskDTOs.CalculateRiskRequest request) {
        try {
            String country = request.clientRiskRatingRequest()
                    .get(0).clientDetails().clientAdoptionCountry();
            return "CU".equalsIgnoreCase(country) || "Cuba".equalsIgnoreCase(country);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractRecordId(RiskDTOs.CalculateRiskRequest request) {
        try {
            return request.clientRiskRatingRequest().get(0).clientDetails().recordID();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String extractAdoptionCountry(RiskDTOs.CalculateRiskRequest request) {
        try {
            return request.clientRiskRatingRequest().get(0).clientDetails().clientAdoptionCountry();
        } catch (Exception e) {
            return "DE";
        }
    }
}
