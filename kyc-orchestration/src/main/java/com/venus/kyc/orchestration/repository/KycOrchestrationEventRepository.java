package com.venus.kyc.orchestration.repository;

import com.venus.kyc.orchestration.domain.KycOrchestrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycOrchestrationEventRepository extends JpaRepository<KycOrchestrationEvent, Long> {

    List<KycOrchestrationEvent> findByKycTransactionIdOrderByCreatedAt(Long kycTransactionId);
}
