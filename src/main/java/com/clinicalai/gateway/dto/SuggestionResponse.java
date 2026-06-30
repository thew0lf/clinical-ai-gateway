package com.clinicalai.gateway.dto;

import com.clinicalai.gateway.domain.AssistantType;
import com.clinicalai.gateway.domain.Suggestion;
import com.clinicalai.gateway.domain.SuggestionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Outbound view of a suggestion, including its provenance and decision metadata. */
public record SuggestionResponse(
    UUID id,
    String tenantId,
    String encounterId,
    AssistantType assistantType,
    SuggestionStatus status,
    String promptRedacted,
    String modelProvider,
    String modelOutput,
    int tokensUsed,
    String requestedBy,
    String decidedBy,
    String decisionReason,
    Instant createdAt,
    Instant decidedAt,
    List<GroundingReferenceResponse> grounding) {

  public static SuggestionResponse from(Suggestion s) {
    return new SuggestionResponse(
        s.getId(),
        s.getTenantId(),
        s.getEncounterId(),
        s.getAssistantType(),
        s.getStatus(),
        s.getPromptRedacted(),
        s.getModelProvider(),
        s.getModelOutput(),
        s.getTokensUsed(),
        s.getRequestedBy(),
        s.getDecidedBy(),
        s.getDecisionReason(),
        s.getCreatedAt(),
        s.getDecidedAt(),
        s.getGroundingReferences().stream().map(GroundingReferenceResponse::from).toList());
  }
}
