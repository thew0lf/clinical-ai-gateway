package com.clinicalai.gateway.domain;

/**
 * The class of AI Assistant that produced a suggestion. Mirrors the assistant fleet in the
 * AI-Powered Practice model (clinical, billing, front office, patient, support).
 */
public enum AssistantType {
  BILLING,
  CLINICAL,
  FRONT_OFFICE,
  PATIENT,
  SUPPORT
}
