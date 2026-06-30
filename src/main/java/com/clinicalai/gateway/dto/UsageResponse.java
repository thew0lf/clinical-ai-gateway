package com.clinicalai.gateway.dto;

/**
 * Per tenant usage snapshot for the cost and abuse observability dashboard. Reflects the soft budget
 * posture of the control plane: usage is reported, not enforced, unless the mode is ENFORCE.
 */
public record UsageResponse(
    String tenantId,
    String mode,
    int requestBudgetPerMinute,
    long requestsThisMinute,
    long tokensToday) {}
