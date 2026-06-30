package com.clinicalai.gateway.service.model;

/** Result of a model completion: the output text, the provider that served it, and token usage. */
public record ModelResult(String output, String provider, int tokensUsed) {}
