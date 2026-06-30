package com.clinicalai.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.clinicalai.gateway.domain.AuditEventRecord;
import com.clinicalai.gateway.domain.SuggestionStatus;
import com.clinicalai.gateway.dto.SuggestionResponse;
import com.clinicalai.gateway.repository.AuditEventRepository;
import com.clinicalai.gateway.security.JwtMinter;
import com.clinicalai.gateway.security.PemKeys;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End to end test of the governed flow against real Postgres, Kafka, and Redis containers. It
 * creates a suggestion, approves it, and then waits for the Kafka audit consumer to persist the
 * audit trail, proving the producer, broker, consumer, and read model are wired correctly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = "app.governance.review-required-assistants=BILLING")
class SuggestionFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  @Container @ServiceConnection
  static KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired private TestRestTemplate rest;
  @Autowired private AuditEventRepository auditRepository;

  private String token() {
    JwtMinter minter = new JwtMinter(PemKeys.loadPrivateKey("keys/app.key"));
    return minter.mint(
        "clinician@test",
        "tenant-itest",
        List.of("suggestion:create", "suggestion:approve", "suggestion:execute"),
        Duration.ofHours(1));
  }

  @Test
  void createApproveAndAudit() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token());
    headers.setContentType(MediaType.APPLICATION_JSON);

    String createBody =
        """
        {
          "encounterId": "enc-itest-1",
          "assistantType": "BILLING",
          "inputText": "Patient SSN 123-45-6789 seen for follow up.",
          "groundingReferences": [
            {"sourceType": "ENCOUNTER", "sourceId": "enc-itest-1", "description": "visit note"}
          ]
        }
        """;

    ResponseEntity<SuggestionResponse> created =
        rest.exchange(
            "/api/v1/suggestions",
            HttpMethod.POST,
            new HttpEntity<>(createBody, headers),
            SuggestionResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    SuggestionResponse body = created.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(SuggestionStatus.PENDING);
    assertThat(body.modelProvider()).isEqualTo("mock");
    assertThat(body.modelOutput()).isNotBlank();
    assertThat(body.tokensUsed()).isGreaterThan(0);
    assertThat(body.grounding()).hasSize(1);

    UUID id = body.id();

    HttpHeaders authOnly = new HttpHeaders();
    authOnly.setBearerAuth(token());

    ResponseEntity<SuggestionResponse> approved =
        rest.exchange(
            "/api/v1/suggestions/" + id + "/approve",
            HttpMethod.POST,
            new HttpEntity<>(authOnly),
            SuggestionResponse.class);

    assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(approved.getBody()).isNotNull();
    assertThat(approved.getBody().status()).isEqualTo(SuggestionStatus.APPROVED);

    await()
        .atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(
            () -> {
              List<AuditEventRecord> events =
                  auditRepository.findBySuggestionIdOrderByOccurredAtAsc(id);
              assertThat(events)
                  .extracting(AuditEventRecord::getEventType)
                  .contains("SUGGESTION_CREATED", "SUGGESTION_APPROVED");
            });
  }
}
