package com.clinicalai.gateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted read model of an audit event. Written by the Kafka audit consumer so that the audit
 * trail survives independently of the producing transaction and can be queried for compliance.
 */
@Entity
@Table(name = "audit_event")
public class AuditEventRecord {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "suggestion_id", nullable = false)
  private UUID suggestionId;

  @Column(nullable = false)
  private String actor;

  @Column(columnDefinition = "text")
  private String details;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected AuditEventRecord() {
    // for JPA
  }

  public AuditEventRecord(
      UUID eventId,
      String eventType,
      String tenantId,
      UUID suggestionId,
      String actor,
      String details,
      Instant occurredAt) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.tenantId = tenantId;
    this.suggestionId = suggestionId;
    this.actor = actor;
    this.details = details;
    this.occurredAt = occurredAt;
    this.receivedAt = Instant.now();
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getTenantId() {
    return tenantId;
  }

  public UUID getSuggestionId() {
    return suggestionId;
  }

  public String getActor() {
    return actor;
  }

  public String getDetails() {
    return details;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}
