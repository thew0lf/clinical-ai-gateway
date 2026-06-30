# 6. Usage metering and soft budgets, not hard rate limits

## Status

Accepted

## Context

The control plane needs visibility into cost and abuse across the assistant fleet, since model calls
carry real spend and a single tenant can drive a large volume. An obvious approach is a hard rate
limit that rejects requests once a threshold is crossed. In this product, however, blocking a
clinician or a patient facing assistant in the middle of a workflow is a poor outcome, and a hard cap
turns a cost concern into a reliability and experience problem.

## Decision

The default posture is metering, not throttling. The control plane records per tenant request counts
and token usage in Redis and logs when a tenant crosses a soft budget, without blocking the request.
This keeps spend and abuse observable and ready to feed dashboards and alerts. A hard enforcement
mode is available behind configuration, app.usage.mode set to enforce, for tenants or environments
that explicitly opt into a cap. Enforcement is off by default.

## Consequences

AI requests are not blocked by default, so the assistants stay responsive and the cost signal is
captured where it can drive decisions rather than outages. Operators who need a hard ceiling can
enable enforcement per environment without code change. The tradeoff is that metering alone does not
prevent a runaway tenant from incurring spend in the moment, so it must be paired with alerting and,
where required, the opt in enforcement mode. The token counters also provide the foundation for per
tenant cost reporting and for a future token budget that uses the same key strategy.
