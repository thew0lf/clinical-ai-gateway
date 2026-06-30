package com.clinicalai.gateway.web;

import com.clinicalai.gateway.domain.Suggestion;
import com.clinicalai.gateway.dto.CreateSuggestionRequest;
import com.clinicalai.gateway.dto.DecisionRequest;
import com.clinicalai.gateway.dto.SuggestionResponse;
import com.clinicalai.gateway.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST surface for the governed suggestion lifecycle. Tenant and actor are read from the JWT. */
@RestController
@RequestMapping("/api/v1/suggestions")
@Tag(name = "Suggestions", description = "Governed clinical AI suggestion lifecycle")
public class SuggestionController {

  private final SuggestionService service;

  public SuggestionController(SuggestionService service) {
    this.service = service;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_suggestion:create')")
  @Operation(summary = "Generate a governed AI suggestion in PENDING state")
  public ResponseEntity<SuggestionResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateSuggestionRequest request) {
    Suggestion created = service.create(tenant(jwt), actor(jwt), request);
    return ResponseEntity.created(URI.create("/api/v1/suggestions/" + created.getId()))
        .body(SuggestionResponse.from(created));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Fetch a suggestion with its provenance and decision metadata")
  public SuggestionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return SuggestionResponse.from(service.get(tenant(jwt), id));
  }

  @GetMapping
  @Operation(summary = "List suggestions for the caller tenant")
  public List<SuggestionResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return service.list(tenant(jwt)).stream().map(SuggestionResponse::from).toList();
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('SCOPE_suggestion:approve')")
  @Operation(summary = "Approve a pending suggestion")
  public SuggestionResponse approve(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody(required = false) DecisionRequest body) {
    return SuggestionResponse.from(service.approve(tenant(jwt), actor(jwt), id));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('SCOPE_suggestion:approve')")
  @Operation(summary = "Reject a pending suggestion with a reason")
  public SuggestionResponse reject(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody(required = false) DecisionRequest body) {
    String reason = body == null ? null : body.reason();
    return SuggestionResponse.from(service.reject(tenant(jwt), actor(jwt), id, reason));
  }

  @PostMapping("/{id}/execute")
  @PreAuthorize("hasAuthority('SCOPE_suggestion:execute')")
  @Operation(summary = "Mark an approved suggestion as executed downstream")
  public SuggestionResponse execute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return SuggestionResponse.from(service.execute(tenant(jwt), actor(jwt), id));
  }

  private String tenant(Jwt jwt) {
    String tenant = jwt.getClaimAsString("tenant_id");
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalStateException("token is missing the tenant_id claim");
    }
    return tenant;
  }

  private String actor(Jwt jwt) {
    return jwt.getSubject();
  }
}
