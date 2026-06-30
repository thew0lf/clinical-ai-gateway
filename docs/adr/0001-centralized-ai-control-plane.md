# 1. Centralized AI control plane

## Status

Accepted

## Context

The product direction is a fleet of AI assistants spanning billing, clinical, front office,
patient, and support workflows. Each assistant needs the same cross cutting controls: protection of
protected health information, per tenant cost and abuse limits, a human review step for any action
that affects a patient record, provenance for every output, and a complete audit trail. If each
assistant implements these controls on its own, the controls drift apart, every new assistant
repeats the same work, and a compliance reviewer has no single place to verify behavior.

## Decision

We introduce a control plane that sits between the assistant surfaces and the model and agent layer.
Assistants do not call the model directly. They call the control plane, which enforces redaction,
usage metering, the review policy, provenance capture, and audit publishing, then delegates
the model call through a provider seam. The control plane owns the governed lifecycle of a
suggestion from creation to a terminal decision.

## Consequences

Governance is defined once and applied uniformly, so a new assistant inherits the controls for free
and a reviewer has a single system of record. The control plane becomes a shared dependency on the
request path, so it must be horizontally scalable and highly available, and its latency budget must
stay small. These properties are addressed by keeping the plane stateless at the web tier, backing
state in Postgres, and keeping the model call behind an interface so it can be made asynchronous if
the latency budget requires it.
