package com.clinicalai.gateway.repository;

import com.clinicalai.gateway.domain.AuditEventRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventRecord, UUID> {

  List<AuditEventRecord> findBySuggestionIdAndTenantIdOrderByOccurredAtAsc(
      UUID suggestionId, String tenantId);

  List<AuditEventRecord> findByTenantIdOrderByOccurredAtDesc(String tenantId, Pageable pageable);
}
