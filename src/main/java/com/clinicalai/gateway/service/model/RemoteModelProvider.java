package com.clinicalai.gateway.service.model;

import com.clinicalai.gateway.domain.AssistantType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder for a real model backend, selected via app.model.provider=remote. The class
 * demonstrates the provider seam without binding the control plane to any single vendor. To make it
 * live, inject an HTTP client or a vendor SDK here, issue the completion call, and map the response
 * into a ModelResult. A routing or fallback strategy can sit in front of several of these without
 * any change to the governance code, since callers depend only on the ModelProvider interface.
 */
@Component
@ConditionalOnProperty(name = "app.model.provider", havingValue = "remote")
public class RemoteModelProvider implements ModelProvider {

  @Override
  public ModelResult complete(AssistantType assistantType, String redactedPrompt) {
    throw new UnsupportedOperationException(
        "Remote provider is a stub. Wire a model client here and map the response to ModelResult.");
  }
}
