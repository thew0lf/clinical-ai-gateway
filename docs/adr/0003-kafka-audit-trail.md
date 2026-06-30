# 3. Kafka for the audit trail

## Status

Accepted

## Context

Every state change in the control plane must produce a durable audit record for compliance under
frameworks such as HIPAA and SOC 2. The audit trail must outlive the request, must be queryable, and
should be available to more than one consumer over time, for example a compliance store, analytics,
and alerting. We considered writing audit rows directly to Postgres in the same transaction, and we
considered a traditional message broker such as RabbitMQ.

## Decision

State changes publish an audit event to a Kafka topic, keyed by suggestion id so that the history of
a single suggestion stays ordered within a partition. A dedicated consumer persists events into an
audit read model in Postgres. Kafka is chosen because it retains the event log, allows independent
consumer groups to read the same stream without coupling, and matches the event driven direction of
the wider platform.

## Consequences

The audit trail is decoupled from the request transaction and can fan out to new consumers without
change to the producer. The cost is an eventual consistency window between a decision and its
persisted audit record, and a dual write between the database and Kafka on the request path. The
production answer to the dual write is a transactional outbox: the event is written to an outbox
table inside the same database transaction as the state change, and a relay publishes it to Kafka.
The current code publishes inline for clarity and isolates the publish behind a single component, so
moving to an outbox is a contained change.
