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

    @Column(name = "unique_client_id")
    private String uniqueClientID;

    private String businessLine;
    private String webhookUrl;

    // -------------------------------------------------------------------------
    // Encrypted PII — name & date of birth
    // -------------------------------------------------------------------------

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "first_name")
    private String firstName;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "last_name")
    private String lastName;

    @Convert(converter = AttributeEncryptor.class)
    private String dob;

    // -------------------------------------------------------------------------
    // Biographical / citizenship
    // -------------------------------------------------------------------------

    @Column(name = "city_of_birth")
    private String cityOfBirth;

    @Column(name = "country_of_birth")
    private String countryOfBirth;

    @Column(name = "primary_citizenship")
    private String primaryCitizenship;

    @Column(name = "second_citizenship")
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

    @Column(name = "country_of_residence")
    private String countryOfResidence;

    private String occupation;

    // -------------------------------------------------------------------------
    // Legitimisation document
    // -------------------------------------------------------------------------

    @Column(name = "type_of_legitimization_document")
    private String typeOfLegitimizationDocument;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "expiration_date")
    private String expirationDate;

    // -------------------------------------------------------------------------
    // Tax identifier (encrypted)
    // -------------------------------------------------------------------------

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "german_tax_id")
    private String germanTaxID;

    // -------------------------------------------------------------------------
    // Orchestration outcome
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_status")
    private ScreeningStatus screeningStatus;

    @Convert(converter = StringListConverter.class)
    @Column(name = "screening_context")
    private List<String> screeningContext;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating")
    private RiskRating riskRating;

    // -------------------------------------------------------------------------
    // External trace IDs
    // -------------------------------------------------------------------------

    @Column(name = "viewer_user_id")
    private String viewerUserId;

    @Column(name = "screening_request_id")
    private String screeningRequestId;

    @Column(name = "risk_request_id")
    private String riskRequestId;

    // -------------------------------------------------------------------------
    // Latency telemetry
    // -------------------------------------------------------------------------

    @Column(name = "screening_start_at")
    private LocalDateTime screeningStartAt;

    @Column(name = "screening_end_at")
    private LocalDateTime screeningEndAt;

    @Column(name = "risk_start_at")
    private LocalDateTime riskStartAt;

    @Column(name = "risk_end_at")
    private LocalDateTime riskEndAt;

    // -------------------------------------------------------------------------
    // Audit timestamps
    // -------------------------------------------------------------------------

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
