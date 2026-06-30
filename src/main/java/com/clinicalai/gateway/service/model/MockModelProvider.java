package com.clinicalai.gateway.service.model;

import com.clinicalai.gateway.domain.AssistantType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic provider used for local development and tests. It returns a canned, specialty
 * shaped response per assistant type so that the full governance flow can be exercised end to end
 * without calling an external model. Selected by default via app.model.provider=mock.
 */
@Component
@ConditionalOnProperty(name = "app.model.provider", havingValue = "mock", matchIfMissing = true)
public class MockModelProvider implements ModelProvider {

  @Override
  public ModelResult complete(AssistantType assistantType, String redactedPrompt) {
    String output =
        switch (assistantType) {
          case BILLING ->
              "Suggested coding for the encounter: ICD-10 L70.0, CPT 99213."
                  + " Rationale derived from the documented assessment.";
          case CLINICAL ->
              "Pre-visit summary: stable follow up. No new acute findings in the provided context.";
          case FRONT_OFFICE ->
              "Proposed action: schedule a follow up in 4 weeks and send an intake reminder.";
          case PATIENT ->
              "Draft patient reply: your request has been received and routed to the care team.";
          case SUPPORT ->
              "Suggested resolution: verify user role and permissions, then re-run the report.";
        };
    int tokensUsed = estimateTokens(redactedPrompt) + estimateTokens(output);
    return new ModelResult(output, "mock", tokensUsed);
  }

  private int estimateTokens(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }
}
