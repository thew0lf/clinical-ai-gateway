package com.clinicalai.gateway.dto;

import com.clinicalai.gateway.domain.AuditEventRecord;
import java.time.Instant;
import java.util.UUID;

/** Outbound view of a persisted audit event from the compliance read model. */
public record AuditEventResponse(
    UUID eventId,
    String eventType,
    String tenantId,
    UUID suggestionId,
    String actor,
    String details,
    Instant occurredAt) {

  public static AuditEventResponse from(AuditEventRecord r) {
    return new AuditEventResponse(
        r.getEventId(),
        r.getEventType(),
        r.getTenantId(),
        r.getSuggestionId(),
        r.getActor(),
        r.getDetails(),
        r.getOccurredAt());
  }
}
