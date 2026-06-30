# 4. PHI redaction at the gateway boundary

## Status

Accepted

## Context

Assistant inputs can contain protected health information. Model providers may run outside our trust
boundary, and even a provider covered by a business associate agreement should receive the minimum
necessary data. We need a consistent point at which direct identifiers are removed before any model
call, rather than trusting each assistant or each provider contract to handle this correctly.

## Decision

The control plane redacts direct identifiers from inbound text at the gateway boundary, before the
model provider is called, for every assistant. Redaction is performed by a single service behind a
stable interface. The current implementation is pattern based and illustrative. A production
deployment replaces the implementation with a dedicated clinical detection service behind the same
interface, without changing the call sites.

## Consequences

Redaction is applied once, uniformly, and is independent of any provider contract, which reduces the
data exposed across the whole assistant fleet and gives one place to test and improve detection. The
tradeoffs are the risk of over redaction reducing model quality, and the fact that pattern based
detection is not sufficient on its own for free text clinical notes. Relying on a provider business
associate agreement alone was rejected, because a contract does not reduce the data sent and does not
give us control over detection quality.
