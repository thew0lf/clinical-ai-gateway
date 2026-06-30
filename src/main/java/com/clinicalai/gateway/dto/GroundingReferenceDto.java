package com.clinicalai.gateway.dto;

import com.clinicalai.gateway.domain.GroundingSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Inbound provenance reference attached to a suggestion request. */
public record GroundingReferenceDto(
    @NotNull GroundingSourceType sourceType,
    @NotBlank String sourceId,
    String description) {}
