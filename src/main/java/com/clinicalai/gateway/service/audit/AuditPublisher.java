package com.clinicalai.gateway.service.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes audit events to Kafka, keyed by suggestion id so a suggestion history stays ordered. */
@Component
public class AuditPublisher {

  private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String topic;

  public AuditPublisher(
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("${app.kafka.audit-topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void publish(AuditEvent event) {
    log.debug(
        "publishing audit event type={} suggestion={} tenant={}",
        event.eventType(),
        event.suggestionId(),
        event.tenantId());
    kafkaTemplate.send(topic, event.suggestionId().toString(), event);
  }
}
