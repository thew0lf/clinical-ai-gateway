# 2. Policy driven human review

## Status

Accepted

## Context

Assistants produce suggestions that range from low impact drafts to clinical and financial actions.
A blanket rule that every AI output must be held for human approval would slow the low impact paths
without adding safety, and a blanket rule that nothing is reviewed would be unsafe for the high
impact paths. The control plane needs accountability for every output, but the level of human
oversight should match the risk of the action rather than being fixed for all assistants.

## Decision

Review is a policy decision, not a hard gate. The control plane evaluates a governance policy at
creation time and decides whether a suggestion needs human review. When review is not required the
suggestion is auto approved and the assistant proceeds autonomously. When review is required the
suggestion stays in PENDING until an authorized reviewer approves or rejects it. The policy is
configuration driven through app.governance.review-required-assistants, which is empty by default,
so the default posture is autonomy. A risk based list such as CLINICAL,BILLING turns on review for
exactly those flows. The state machine and the audit trail apply on every path, including auto
approval, so accountability is preserved regardless of the policy.

## Consequences

The system does not impose friction where it adds no value, and it can require human oversight
exactly where the risk justifies it, all without code change. Every decision, automated or human, is
recorded with an actor, which keeps the audit trail complete. The tradeoff is that the policy itself
becomes a governed artifact that must be reviewed and tested, since it determines where autonomy
applies. The current policy keys on assistant type. It can be extended to consider action risk,
model confidence, or tenant configuration behind the same interface.
