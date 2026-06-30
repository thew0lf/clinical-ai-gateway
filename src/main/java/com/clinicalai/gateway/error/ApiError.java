package com.clinicalai.gateway.error;

import java.time.Instant;

/** Minimal problem response body returned for handled error conditions. */
public record ApiError(Instant timestamp, int status, String error, String message) {}
