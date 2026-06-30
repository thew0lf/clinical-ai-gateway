package com.clinicalai.gateway.service.audit;

import com.clinicalai.gateway.domain.AuditEventRecord;
import com.clinicalai.gateway.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Persists audit events into the read model. Running this as a separate Kafka consumer decouples the
 * durable compliance store from the request transaction, and lets additional consumers (analytics,
 * billing, alerting) subscribe to the same topic independently.
 */
@Component
public class AuditConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

  private final AuditEventRepository repository;

  public AuditConsumer(AuditEventRepository repository) {
    this.repository = repository;
  }

  @KafkaListener(topics = "${app.kafka.audit-topic}", groupId = "audit-persister")
  public void onAuditEvent(AuditEvent event) {
    if (repository.existsById(event.eventId())) {
      return;
    }
    AuditEventRecord record =
        new AuditEventRecord(
            event.eventId(),
            event.eventType(),
            event.tenantId(),
            event.suggestionId(),
            event.actor(),
            event.details(),
            event.occurredAt());
    repository.save(record);
    log.debug("persisted audit event {}", event.eventId());
  }
}
