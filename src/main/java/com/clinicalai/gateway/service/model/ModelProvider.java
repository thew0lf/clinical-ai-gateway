package com.clinicalai.gateway.service.model;

import com.clinicalai.gateway.domain.AssistantType;

/**
 * Seam over the model backend. The gateway depends only on this interface so that the provider
 * (a mock, a remote backend, or any other) can be swapped without touching governance logic, and so
 * a routing or fallback strategy can sit in front of multiple providers.
 */
public interface ModelProvider {

  ModelResult complete(AssistantType assistantType, String redactedPrompt);
}
