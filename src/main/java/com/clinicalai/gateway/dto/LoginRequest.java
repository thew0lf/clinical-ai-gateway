package com.clinicalai.gateway.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials for the local demo login that stands in for an external identity provider. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
