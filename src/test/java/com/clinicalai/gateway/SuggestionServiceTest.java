package com.clinicalai.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clinicalai.gateway.domain.AssistantType;
import com.clinicalai.gateway.domain.Suggestion;
import com.clinicalai.gateway.domain.SuggestionStatus;
import com.clinicalai.gateway.dto.CreateSuggestionRequest;
import com.clinicalai.gateway.error.RateLimitExceededException;
import com.clinicalai.gateway.repository.SuggestionRepository;
import com.clinicalai.gateway.service.GovernancePolicy;
import com.clinicalai.gateway.service.RedactionService;
import com.clinicalai.gateway.service.SuggestionService;
import com.clinicalai.gateway.service.UsageMeteringService;
import com.clinicalai.gateway.service.audit.AuditEvent;
import com.clinicalai.gateway.service.audit.AuditEventType;
import com.clinicalai.gateway.service.audit.AuditPublisher;
import com.clinicalai.gateway.service.model.ModelProvider;
import com.clinicalai.gateway.service.model.ModelResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

  @Mock private SuggestionRepository repository;
  @Mock private UsageMeteringService usageMeteringService;
  @Mock private RedactionService redactionService;
  @Mock private ModelProvider modelProvider;
  @Mock private GovernancePolicy governancePolicy;
  @Mock private AuditPublisher auditPublisher;

  private SuggestionService service() {
    return new SuggestionService(
        repository,
        usageMeteringService,
        redactionService,
        modelProvider,
        governancePolicy,
        auditPublisher);
  }

  private CreateSuggestionRequest request() {
    return new CreateSuggestionRequest(
        "enc-1", AssistantType.BILLING, "raw text with SSN 123-45-6789", List.of());
  }

  @Test
  void createMetersRedactsCallsModelAndPersists() {
    when(redactionService.redact(anyString())).thenReturn("redacted text");
    when(modelProvider.complete(eq(AssistantType.BILLING), eq("redacted text")))
        .thenReturn(new ModelResult("coded output", "mock", 12));
    when(repository.save(any(Suggestion.class))).thenAnswer(inv -> inv.getArgument(0));
    when(governancePolicy.requiresReview(AssistantType.BILLING)).thenReturn(true);

    Suggestion result = service().create("tenant-a", "actor-1", request());

    assertThat(result.getStatus()).isEqualTo(SuggestionStatus.PENDING);
    assertThat(result.getModelProvider()).isEqualTo("mock");
    verify(usageMeteringService).admit("tenant-a");
    verify(usageMeteringService).recordTokens("tenant-a", 12);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPublisher, times(1)).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(AuditEventType.SUGGESTION_CREATED);
  }

  @Test
  void createAutoApprovesWhenPolicyAllowsAutonomy() {
    when(redactionService.redact(anyString())).thenReturn("redacted text");
    when(modelProvider.complete(any(), anyString()))
        .thenReturn(new ModelResult("coded output", "mock", 8));
    when(repository.save(any(Suggestion.class))).thenAnswer(inv -> inv.getArgument(0));
    when(governancePolicy.requiresReview(AssistantType.BILLING)).thenReturn(false);

    Suggestion result = service().create("tenant-a", "actor-1", request());

    assertThat(result.getStatus()).isEqualTo(SuggestionStatus.APPROVED);
    assertThat(result.getDecidedBy()).isEqualTo("policy:auto");

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPublisher, times(2)).publish(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(AuditEvent::eventType)
        .containsExactly(
            AuditEventType.SUGGESTION_CREATED, AuditEventType.SUGGESTION_AUTO_APPROVED);
  }

  @Test
  void createStopsWhenUsageEnforcementRejects() {
    doThrow(new RateLimitExceededException("over")).when(usageMeteringService).admit(anyString());

    assertThatThrownBy(() -> service().create("tenant-a", "actor-1", request()))
        .isInstanceOf(RateLimitExceededException.class);

    verify(modelProvider, never()).complete(any(), anyString());
    verify(usageMeteringService, never()).recordTokens(anyString(), anyInt());
    verify(repository, never()).save(any());
    verify(auditPublisher, never()).publish(any());
  }

  @Test
  void approveTwiceIsRejectedByStateMachine() {
    Suggestion suggestion =
        Suggestion.create(
            "tenant-a", "enc-1", AssistantType.BILLING, "r", "mock", "o", 5, "actor-1");
    suggestion.approve("reviewer");
    UUID id = suggestion.getId();
    when(repository.findByIdAndTenantId(id, "tenant-a")).thenReturn(Optional.of(suggestion));

    assertThatThrownBy(() -> service().approve("tenant-a", "reviewer", id))
        .isInstanceOf(IllegalStateException.class);
  }
}
