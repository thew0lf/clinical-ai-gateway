package com.clinicalai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Clinical AI Control Plane. A governance gateway that brokers AI assistant requests, enforces PHI
 * redaction and per-tenant rate limits, persists a human-in-the-loop approval state machine, and
 * publishes an immutable audit trail to Kafka.
 */
@SpringBootApplication
@EnableKafka
public class ClinicalAiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClinicalAiGatewayApplication.class, args);
  }
}
