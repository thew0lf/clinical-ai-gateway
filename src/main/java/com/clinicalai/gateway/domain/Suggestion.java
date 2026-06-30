package com.clinicalai.gateway.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single AI suggestion under governance. The entity is the source of truth for the
 * human-in-the-loop approval state machine and carries provenance plus the model metadata needed
 * for audit.
 */
@Entity
@Table(name = "suggestion")
public class Suggestion {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "encounter_id", nullable = false)
  private String encounterId;

  @Enumerated(EnumType.STRING)
  @Column(name = "assistant_type", nullable = false)
  private AssistantType assistantType;

  @Column(name = "prompt_redacted", nullable = false, columnDefinition = "text")
  private String promptRedacted;

  @Column(name = "model_provider", nullable = false)
  private String modelProvider;

  @Column(name = "model_output", nullable = false, columnDefinition = "text")
  private String modelOutput;

  @Column(name = "tokens_used", nullable = false)
  private int tokensUsed;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SuggestionStatus status;

  @Column(name = "requested_by", nullable = false)
  private String requestedBy;

  @Column(name = "decided_by")
  private String decidedBy;

  @Column(name = "decision_reason", columnDefinition = "text")
  private String decisionReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Version
  @Column(nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "suggestion",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<GroundingReference> groundingReferences = new ArrayList<>();

  protected Suggestion() {
    // for JPA
  }

  private Suggestion(
      String tenantId,
      String encounterId,
      AssistantType assistantType,
      String promptRedacted,
      String modelProvider,
      String modelOutput,
      int tokensUsed,
      String requestedBy) {
    this.id = UUID.randomUUID();
    this.tenantId = tenantId;
    this.encounterId = encounterId;
    this.assistantType = assistantType;
    this.promptRedacted = promptRedacted;
    this.modelProvider = modelProvider;
    this.modelOutput = modelOutput;
    this.tokensUsed = tokensUsed;
    this.requestedBy = requestedBy;
    this.status = SuggestionStatus.PENDING;
    this.createdAt = Instant.now();
  }

  /** Factory for a freshly created suggestion in PENDING state. */
  public static Suggestion create(
      String tenantId,
      String encounterId,
      AssistantType assistantType,
      String promptRedacted,
      String modelProvider,
      String modelOutput,
      int tokensUsed,
      String requestedBy) {
    return new Suggestion(
        tenantId,
        encounterId,
        assistantType,
        promptRedacted,
        modelProvider,
        modelOutput,
        tokensUsed,
        requestedBy);
  }

  public void addGrounding(GroundingReference reference) {
    reference.setSuggestion(this);
    this.groundingReferences.add(reference);
  }

  /** Approve a pending suggestion. */
  public void approve(String actor) {
    requirePending();
    this.status = SuggestionStatus.APPROVED;
    this.decidedBy = actor;
    this.decidedAt = Instant.now();
  }

  /** Reject a pending suggestion with a reason. */
  public void reject(String actor, String reason) {
    requirePending();
    this.status = SuggestionStatus.REJECTED;
    this.decidedBy = actor;
    this.decisionReason = reason;
    this.decidedAt = Instant.now();
  }

  /** Mark an approved suggestion as executed by the downstream system. */
  public void markExecuted() {
    if (this.status != SuggestionStatus.APPROVED) {
      throw new IllegalStateException(
          "only an APPROVED suggestion can be executed, current status is " + this.status);
    }
    this.status = SuggestionStatus.EXECUTED;
  }

  private void requirePending() {
    if (this.status != SuggestionStatus.PENDING) {
      throw new IllegalStateException(
          "decision is only allowed from PENDING, current status is " + this.status);
    }
  }

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEncounterId() {
    return encounterId;
  }

  public AssistantType getAssistantType() {
    return assistantType;
  }

  public String getPromptRedacted() {
    return promptRedacted;
  }

  public String getModelProvider() {
    return modelProvider;
  }

  public String getModelOutput() {
    return modelOutput;
  }

  public int getTokensUsed() {
    return tokensUsed;
  }

  public SuggestionStatus getStatus() {
    return status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public long getVersion() {
    return version;
  }

  public List<GroundingReference> getGroundingReferences() {
    return groundingReferences;
  }
}
