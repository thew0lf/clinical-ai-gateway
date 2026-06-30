package com.clinicalai.gateway.service.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record published to Kafka for every state change in the control plane. The event
 * is the unit of the compliance trail: who did what, to which suggestion, for which tenant, when.
 */
public record AuditEvent(
    UUID eventId,
    String eventType,
    String tenantId,
    UUID suggestionId,
    String actor,
    String details,
    Instant occurredAt) {

  public static AuditEvent of(
      String eventType, String tenantId, UUID suggestionId, String actor, String details) {
    return new AuditEvent(
        UUID.randomUUID(), eventType, tenantId, suggestionId, actor, details, Instant.now());
  }
}
