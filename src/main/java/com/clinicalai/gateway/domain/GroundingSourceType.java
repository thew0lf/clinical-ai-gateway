package com.clinicalai.gateway.domain;

/**
 * The kind of clinical record a suggestion was grounded in. Provenance is recorded so that every
 * AI output can be traced back to the source data the model was given.
 */
public enum GroundingSourceType {
  ENCOUNTER,
  LAB_RESULT,
  CLINICAL_NOTE,
  PATHOLOGY,
  MEDICATION,
  DEMOGRAPHIC
}
