package com.venus.kyc.orchestration.domain;

import com.venus.kyc.orchestration.crypto.AttributeEncryptor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only event in one KYC transaction's lifecycle (INITIATED, SCREENING_RESULT,
 * RISK_RESULT, FINALIZED, STATUS_CHANGED). Unlike KycTransactionAudit (which only tracks
 * current state and is mutated in place), every call site here inserts a new row — so the
 * full history, including each downstream service's raw response, is always queryable.
 */
@Entity
@Table(name = "kyc_orchestration_events")
@Getter
@Setter
public class KycOrchestrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kyc_transaction_id")
    private Long kycTransactionId;

    @Column(name = "unique_client_id")
    private String uniqueClientID;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    private String source;

    @Convert(converter = AttributeEncryptor.class)
    @Column(name = "downstream_response")
    private String downstreamResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
