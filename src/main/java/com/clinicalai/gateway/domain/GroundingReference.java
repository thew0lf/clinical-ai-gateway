package com.clinicalai.gateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Provenance record linking a suggestion to a specific source clinical record. */
@Entity
@Table(name = "grounding_reference")
public class GroundingReference {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suggestion_id", nullable = false)
  private Suggestion suggestion;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false)
  private GroundingSourceType sourceType;

  @Column(name = "source_id", nullable = false)
  private String sourceId;

  @Column(columnDefinition = "text")
  private String description;

  protected GroundingReference() {
    // for JPA
  }

  public GroundingReference(GroundingSourceType sourceType, String sourceId, String description) {
    this.id = UUID.randomUUID();
    this.sourceType = sourceType;
    this.sourceId = sourceId;
    this.description = description;
  }

  void setSuggestion(Suggestion suggestion) {
    this.suggestion = suggestion;
  }

  public UUID getId() {
    return id;
  }

  public GroundingSourceType getSourceType() {
    return sourceType;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getDescription() {
    return description;
  }
}
