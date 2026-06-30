# 5. Vendor neutral model provider seam

## Status

Accepted

## Context

The control plane must call a model backend, but the backend will change over time and must not be
hardcoded into the governance logic. We do not want to couple the platform to a single model vendor,
and we need a deterministic backend for local development and tests. We also want the option to route
or fall back across more than one backend without disturbing the governed flow.

## Decision

Model access sits behind a ModelProvider interface. The governance code depends only on this
interface and never references a concrete backend. A mock provider returns deterministic, specialty
shaped output and is the default for local development and tests. A remote provider, selected by
configuration, is the seam where a real backend is wired, whether that is an internal model gateway,
a hosted model API, or a managed cloud service. A routing or fallback strategy can be placed in front
of several providers later, since callers never depend on a concrete one.

## Consequences

The backend can be swapped or composed without any change to redaction, usage metering, the review
policy, or audit. Tests run end to end with no external dependency and no model cost. The choice of
a specific vendor is deferred to deployment configuration rather than baked into the code. The
tradeoffs are one layer of indirection and the need to keep the result contract stable across
providers, so that token accounting and output handling stay uniform. A single hardcoded provider was
rejected, because it would couple governance to one vendor and remove the ability to test the flow
deterministically.
