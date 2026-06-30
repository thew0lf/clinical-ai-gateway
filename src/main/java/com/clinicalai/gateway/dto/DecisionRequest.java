package com.clinicalai.gateway.dto;

/** Optional reason supplied with an approve or reject decision. */
public record DecisionRequest(String reason) {}
