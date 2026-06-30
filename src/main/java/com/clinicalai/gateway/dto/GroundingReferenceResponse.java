package com.clinicalai.gateway.dto;

import com.clinicalai.gateway.domain.GroundingReference;
import com.clinicalai.gateway.domain.GroundingSourceType;

public record GroundingReferenceResponse(
    GroundingSourceType sourceType, String sourceId, String description) {

  public static GroundingReferenceResponse from(GroundingReference reference) {
    return new GroundingReferenceResponse(
        reference.getSourceType(), reference.getSourceId(), reference.getDescription());
  }
}
