package com.venus.kyc.orchestration.repository;

import com.venus.kyc.orchestration.domain.KycTransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycTransactionAuditRepository extends JpaRepository<KycTransactionAudit, Long> {
}
