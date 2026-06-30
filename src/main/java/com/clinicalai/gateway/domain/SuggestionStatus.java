package com.clinicalai.gateway.domain;

/**
 * Lifecycle state of a suggestion. The control plane only permits a fixed set of transitions:
 * PENDING to APPROVED or REJECTED, and APPROVED to EXECUTED.
 */
public enum SuggestionStatus {
  PENDING,
  APPROVED,
  REJECTED,
  EXECUTED
}
