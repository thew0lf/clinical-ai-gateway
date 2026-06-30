package com.clinicalai.gateway.service.audit;

/** Stable audit event type names. */
public final class AuditEventType {
  public static final String SUGGESTION_CREATED = "SUGGESTION_CREATED";
  public static final String SUGGESTION_AUTO_APPROVED = "SUGGESTION_AUTO_APPROVED";
  public static final String SUGGESTION_APPROVED = "SUGGESTION_APPROVED";
  public static final String SUGGESTION_REJECTED = "SUGGESTION_REJECTED";
  public static final String SUGGESTION_EXECUTED = "SUGGESTION_EXECUTED";

  private AuditEventType() {}
}
