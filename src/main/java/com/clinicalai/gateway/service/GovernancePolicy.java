package com.clinicalai.gateway.service;

import com.clinicalai.gateway.domain.AssistantType;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Decides whether a suggestion needs human review before it is treated as accepted. The control
 * plane does not impose a blanket gate on AI. Review is required only for the assistant types listed
 * in app.governance.review-required-assistants, which is empty by default, so autonomy is the
 * default. Setting the property to a risk based list, for example CLINICAL,BILLING, turns on human
 * review for exactly those high impact flows while leaving the rest autonomous.
 */
@Service
public class GovernancePolicy {

  private final Set<AssistantType> reviewRequired;

  public GovernancePolicy(
      @Value("${app.governance.review-required-assistants:}") String reviewRequiredCsv) {
    Set<AssistantType> parsed = EnumSet.noneOf(AssistantType.class);
    if (reviewRequiredCsv != null && !reviewRequiredCsv.isBlank()) {
      for (String token : reviewRequiredCsv.split(",")) {
        String trimmed = token.trim();
        if (!trimmed.isEmpty()) {
          parsed.add(AssistantType.valueOf(trimmed.toUpperCase()));
        }
      }
    }
    this.reviewRequired = parsed;
  }

  public boolean requiresReview(AssistantType assistantType) {
    return reviewRequired.contains(assistantType);
  }
}
