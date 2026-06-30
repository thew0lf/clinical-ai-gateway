package com.clinicalai.gateway.web;

import com.clinicalai.gateway.dto.AuditEventResponse;
import com.clinicalai.gateway.repository.AuditEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read surface over the audit compliance read model. The trail is populated asynchronously by the
 * Kafka audit consumer, so this controller reads a model that is decoupled from the write path.
 * Every query is scoped to the caller tenant.
 */
@RestController
@Tag(name = "Audit", description = "Compliance audit trail read model")
public class AuditController {

  private static final int MAX_LIMIT = 200;

  private final AuditEventRepository repository;

  public AuditController(AuditEventRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/api/v1/audit")
  @Operation(summary = "Recent audit events for the caller tenant, newest first")
  public List<AuditEventResponse> recent(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    int bounded = Math.max(1, Math.min(limit, MAX_LIMIT));
    return repository
        .findByTenantIdOrderByOccurredAtDesc(tenant(jwt), PageRequest.of(0, bounded))
        .stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  @GetMapping("/api/v1/suggestions/{id}/audit")
  @Operation(summary = "Full audit trail for a single suggestion, oldest first")
  public List<AuditEventResponse> forSuggestion(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return repository.findBySuggestionIdAndTenantIdOrderByOccurredAtAsc(id, tenant(jwt)).stream()
        .map(AuditEventResponse::from)
        .toList();
  }

  private String tenant(Jwt jwt) {
    String tenant = jwt.getClaimAsString("tenant_id");
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalStateException("token is missing the tenant_id claim");
    }
    return tenant;
  }
}
