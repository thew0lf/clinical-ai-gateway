package com.clinicalai.gateway.error;

/** Raised when a tenant exceeds its configured request budget for the current window. */
public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException(String message) {
    super(message);
  }
}
