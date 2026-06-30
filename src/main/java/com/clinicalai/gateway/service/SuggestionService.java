package com.clinicalai.gateway.service;

import com.clinicalai.gateway.domain.GroundingReference;
import com.clinicalai.gateway.domain.Suggestion;
import com.clinicalai.gateway.dto.CreateSuggestionRequest;
import com.clinicalai.gateway.dto.GroundingReferenceDto;
import com.clinicalai.gateway.error.SuggestionNotFoundException;
import com.clinicalai.gateway.repository.SuggestionRepository;
import com.clinicalai.gateway.service.audit.AuditEvent;
import com.clinicalai.gateway.service.audit.AuditEventType;
import com.clinicalai.gateway.service.audit.AuditPublisher;
import com.clinicalai.gateway.service.model.ModelProvider;
import com.clinicalai.gateway.service.model.ModelResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the governed lifecycle of an AI suggestion. Every request passes through the same
 * plane: meter the tenant usage for cost observability, redact PHI, call the model behind the
 * provider seam, and persist the suggestion with its provenance. Whether the suggestion then needs a
 * human decision is a policy choice, not a fixed rule. When policy does not require review, the
 * suggestion is auto approved and the assistant proceeds autonomously. Every path is audited.
 *
 * <p>Audit publishing happens inside the write transaction here for clarity. The production grade
 * answer to the dual write between the database and Kafka is a transactional outbox, recorded in
 * ADR 0003.
 */
@Service
public class SuggestionService {

  private static final String POLICY_ACTOR = "policy:auto";

  private final SuggestionRepository repository;
  private final UsageMeteringService usageMeteringService;
  private final RedactionService redactionService;
  private final ModelProvider modelProvider;
  private final GovernancePolicy governancePolicy;
  private final AuditPublisher auditPublisher;

  public SuggestionService(
      SuggestionRepository repository,
      UsageMeteringService usageMeteringService,
      RedactionService redactionService,
      ModelProvider modelProvider,
      GovernancePolicy governancePolicy,
      AuditPublisher auditPublisher) {
    this.repository = repository;
    this.usageMeteringService = usageMeteringService;
    this.redactionService = redactionService;
    this.modelProvider = modelProvider;
    this.governancePolicy = governancePolicy;
    this.auditPublisher = auditPublisher;
  }

  @Transactional
  public Suggestion create(String tenantId, String actor, CreateSuggestionRequest request) {
    usageMeteringService.admit(tenantId);

    String redacted = redactionService.redact(request.inputText());
    ModelResult result = modelProvider.complete(request.assistantType(), redacted);
    usageMeteringService.recordTokens(tenantId, result.tokensUsed());

    Suggestion suggestion =
        Suggestion.create(
            tenantId,
            request.encounterId(),
            request.assistantType(),
            redacted,
            result.provider(),
            result.output(),
            result.tokensUsed(),
            actor);

    for (GroundingReferenceDto ref : request.groundingReferencesOrEmpty()) {
      suggestion.addGrounding(
          new GroundingReference(ref.sourceType(), ref.sourceId(), ref.description()));
    }

    boolean requiresReview = governancePolicy.requiresReview(request.assistantType());
    if (!requiresReview) {
      suggestion.approve(POLICY_ACTOR);
    }

    Suggestion saved = repository.save(suggestion);

    auditPublisher.publish(
        AuditEvent.of(
            AuditEventType.SUGGESTION_CREATED,
            tenantId,
            saved.getId(),
            actor,
            "assistant="
                + request.assistantType()
                + " provider="
                + result.provider()
                + " tokens="
                + result.tokensUsed()
                + " reviewRequired="
                + requiresReview));

    if (!requiresReview) {
      auditPublisher.publish(
          AuditEvent.of(
              AuditEventType.SUGGESTION_AUTO_APPROVED,
              tenantId,
              saved.getId(),
              POLICY_ACTOR,
              "auto approved, policy did not require human review"));
    }
    return saved;
  }

  @Transactional
  public Suggestion approve(String tenantId, String actor, UUID id) {
    Suggestion suggestion = load(tenantId, id);
    suggestion.approve(actor);
    auditPublisher.publish(
        AuditEvent.of(AuditEventType.SUGGESTION_APPROVED, tenantId, id, actor, null));
    return suggestion;
  }

  @Transactional
  public Suggestion reject(String tenantId, String actor, UUID id, String reason) {
    Suggestion suggestion = load(tenantId, id);
    suggestion.reject(actor, reason);
    auditPublisher.publish(
        AuditEvent.of(AuditEventType.SUGGESTION_REJECTED, tenantId, id, actor, reason));
    return suggestion;
  }

  @Transactional
  public Suggestion execute(String tenantId, String actor, UUID id) {
    Suggestion suggestion = load(tenantId, id);
    suggestion.markExecuted();
    auditPublisher.publish(
        AuditEvent.of(AuditEventType.SUGGESTION_EXECUTED, tenantId, id, actor, null));
    return suggestion;
  }

  @Transactional(readOnly = true)
  public Suggestion get(String tenantId, UUID id) {
    return load(tenantId, id);
  }

  @Transactional(readOnly = true)
  public List<Suggestion> list(String tenantId) {
    return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
  }

  private Suggestion load(String tenantId, UUID id) {
    return repository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new SuggestionNotFoundException(id));
  }
}
