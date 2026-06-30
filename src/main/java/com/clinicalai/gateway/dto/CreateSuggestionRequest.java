package com.clinicalai.gateway.dto;

import com.clinicalai.gateway.domain.AssistantType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to generate a governed AI suggestion. The inputText is the clinical context handed to the
 * assistant. It may contain PHI and is redacted at the gateway boundary before any model call.
 */
public record CreateSuggestionRequest(
    @NotBlank String encounterId,
    @NotNull AssistantType assistantType,
    @NotBlank String inputText,
    @Valid List<GroundingReferenceDto> groundingReferences) {

  public List<GroundingReferenceDto> groundingReferencesOrEmpty() {
    return groundingReferences == null ? List.of() : groundingReferences;
  }
}
