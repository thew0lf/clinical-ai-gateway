package com.clinicalai.gateway.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Redacts common direct identifiers from free text before it leaves the trust boundary toward a
 * model provider. This is an illustrative, pattern based redactor. A production deployment would
 * place a dedicated clinical PHI detection service behind this same seam. The point of the design
 * is that redaction happens at the gateway, once, for every assistant, rather than being
 * reimplemented per assistant.
 */
@Service
public class RedactionService {

  private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
  private static final Pattern EMAIL =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
  private static final Pattern PHONE =
      Pattern.compile("\\b(?:\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
  private static final Pattern MRN = Pattern.compile("\\bMRN[:#]?\\s*\\d{5,10}\\b");

  /** Returns the input with direct identifiers replaced by stable placeholder tokens. */
  public String redact(String input) {
    if (input == null || input.isBlank()) {
      return input;
    }
    String result = input;
    result = MRN.matcher(result).replaceAll("[REDACTED_MRN]");
    result = SSN.matcher(result).replaceAll("[REDACTED_SSN]");
    result = EMAIL.matcher(result).replaceAll("[REDACTED_EMAIL]");
    result = PHONE.matcher(result).replaceAll("[REDACTED_PHONE]");
    return result;
  }
}
