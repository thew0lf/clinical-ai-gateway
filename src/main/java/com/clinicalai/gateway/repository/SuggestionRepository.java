package com.clinicalai.gateway.repository;

import com.clinicalai.gateway.domain.Suggestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {

  Optional<Suggestion> findByIdAndTenantId(UUID id, String tenantId);

  List<Suggestion> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
