package com.venus.kyc.orchestration.domain;

import com.venus.kyc.orchestration.crypto.AttributeEncryptor;
import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.domain.enums.RiskRating;
import com.venus.kyc.orchestration.domain.enums.ScreeningStatus;
import com.venus.kyc.orchestration.util.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "kyc_transaction_audit")
@Getter
@Setter
public class KycTransactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // Core identity
    // -------------------------------------------------------------------------

    private String uniqueClientID;
    private String businessLine;
    private String webhookUrl;

    // -------------------------------------------------------------------------
    // Encrypted PII — name & date of birth
    // -------------------------------------------------------------------------

    @Convert(converter = AttributeEncryptor.class)
    private String firstName;

    @Convert(converter = AttributeEncryptor.class)
    private String lastName;

    @Convert(converter = AttributeEncryptor.class)
    private String dob;

    // -------------------------------------------------------------------------
    // Biographical / citizenship
    // -------------------------------------------------------------------------

    private String cityOfBirth;
    private String countryOfBirth;
    private String primaryCitizenship;
    private String secondCitizenship;

    // -------------------------------------------------------------------------
    // Residential address — line1 / line2 encrypted, city & zip plain
    // -------------------------------------------------------------------------

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "addr_line1")
    private String addrLine1;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "addr_line2")
    private String addrLine2;

    @Column(name = "addr_city")
    private String addrCity;

    @Column(name = "addr_zip")
    private String addrZip;

    private String countryOfResidence;
    private String occupation;

    // -------------------------------------------------------------------------
    // Legitimisation document
    // -------------------------------------------------------------------------

    private String typeOfLegitimizationDocument;
    private String issuingAuthority;

    @Convert(converter = AttributeEncryptor.class)
    private String identificationNumber;

    private String expirationDate;

    // -------------------------------------------------------------------------
    // Tax identifier (encrypted)
    // -------------------------------------------------------------------------

    @Convert(converter = AttributeEncryptor.class)
    private String germanTaxID;

    // -------------------------------------------------------------------------
    // Orchestration outcome
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    private ScreeningStatus screeningStatus;

    @Convert(converter = StringListConverter.class)
    @Column(name = "screening_context")
    private List<String> screeningContext;

    @Enumerated(EnumType.STRING)
    private RiskRating riskRating;

    // -------------------------------------------------------------------------
    // External trace IDs
    // -------------------------------------------------------------------------

    private String viewerUserId;
    private String screeningRequestId;
    private String riskRequestId;

    // -------------------------------------------------------------------------
    // Latency telemetry
    // -------------------------------------------------------------------------

    private LocalDateTime screeningStartAt;
    private LocalDateTime screeningEndAt;
    private LocalDateTime riskStartAt;
    private LocalDateTime riskEndAt;

    // -------------------------------------------------------------------------
    // Audit timestamps
    // -------------------------------------------------------------------------

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
