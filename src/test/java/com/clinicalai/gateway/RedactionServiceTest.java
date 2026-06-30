package com.clinicalai.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.clinicalai.gateway.service.RedactionService;
import org.junit.jupiter.api.Test;

class RedactionServiceTest {

  private final RedactionService service = new RedactionService();

  @Test
  void redactsSsnEmailPhoneAndMrn() {
    String input =
        "Patient SSN 123-45-6789, email jane.doe@example.com, phone (561) 555-0199, MRN: 0099221.";
    String redacted = service.redact(input);

    assertThat(redacted).contains("[REDACTED_SSN]");
    assertThat(redacted).contains("[REDACTED_EMAIL]");
    assertThat(redacted).contains("[REDACTED_PHONE]");
    assertThat(redacted).contains("[REDACTED_MRN]");
    assertThat(redacted).doesNotContain("123-45-6789");
    assertThat(redacted).doesNotContain("jane.doe@example.com");
  }

  @Test
  void leavesCleanTextUnchanged() {
    String input = "Stable follow up, no acute findings.";
    assertThat(service.redact(input)).isEqualTo(input);
  }

  @Test
  void handlesNullAndBlank() {
    assertThat(service.redact(null)).isNull();
    assertThat(service.redact("  ")).isEqualTo("  ");
  }
}
