package com.clinicalai.gateway.error;

import java.util.UUID;

/** Raised when a suggestion does not exist for the requesting tenant. */
public class SuggestionNotFoundException extends RuntimeException {
  public SuggestionNotFoundException(UUID id) {
    super("suggestion not found: " + id);
  }
}
