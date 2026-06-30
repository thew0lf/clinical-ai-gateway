# Architecture Process: From Strategy to Running System

Owner: Software Architecture
Status: Working agreement
Audience: Architecture org, engineering leads, product, executive sponsors

This document describes how we take a business strategy and turn it into a scalable,
resilient, secure system without big-bang risk. It is deliberately lightweight. The goal is a
repeatable path from intent to running software that keeps stakeholders aligned and keeps the
architecture decisions explicit and reversible where possible.

The Reviewer Console initiative is used as the worked example throughout, but the process is the
standard we apply to any major initiative.

## Principles

1. **Enable, then govern.** Default to autonomy with observability. Add controls where the risk
   justifies them, not as blanket gates. The architecture should make the safe path the easy path.
2. **Decisions before code.** Every irreversible or expensive choice is written down as an ADR with
   its context, options, and consequences, so the reasoning survives the author.
3. **Thin vertical slices.** Prove the whole flow end to end on a narrow path before widening it.
   A running slice retires more risk than a complete design document.
4. **Cheap artifacts first.** A wireframe costs an hour and surfaces disagreement a sprint of code
   would have hidden. Spend the cheapest artifact that answers the open question.
5. **Architectural runway, not gold plating.** Build only the runway the next two or three
   increments need. Defer the rest and record why.
6. **Seams over vendors.** Bind to interfaces, not products. The model provider, the identity
   provider, and the data store all sit behind seams so they can be swapped without touching the
   governance core.

## The phases

Each phase produces a named artifact and a go / no-go. Phases overlap in practice; the artifacts are
what make the overlap safe.

### 1. Discovery and framing
- **Input:** a business strategy or problem statement from the Director / product.
- **Work:** identify the actors, the jobs to be done, the constraints (compliance, latency, cost,
  data residency), and the non-functional targets. Name what is explicitly out of scope.
- **Artifact:** a one-page **problem and constraints brief**.
- **Gate:** sponsors agree on the problem and the success measures before any design.

### 2. Solution design and decision records
- **Work:** sketch two or three candidate architectures. Evaluate against the non-functionals and
  the multi-generation roadmap. Choose, and capture the why.
- **Artifacts:** a **solution design** (context diagram, component responsibilities, data flow) and
  one **ADR per significant decision**.
- **Gate:** architecture review. Decisions are recorded as Accepted before implementation.

### 3. Experience and interface mockups
- **Work:** for anything with a user or an API surface, produce **wireframes** and the **API
  contract** first. This is the cheapest place to discover that stakeholders pictured different
  things. (See `docs/design/wireframes.md`.)
- **Artifacts:** wireframes and a draft OpenAPI / interface contract.
- **Gate:** product and engineering sign off on the experience and the contract before build.

### 4. Planning and decomposition
- **Work:** break the design into epics and vertically sliced tickets, each with acceptance
  criteria. Sequence so that the riskiest assumption is tested first.
- **Artifact:** a **delivery plan** with epics and tickets. (See
  `docs/planning/backlog.md`.)
- **Gate:** the plan is estimable and the first slice is a thin end-to-end path.

### 5. Walking-skeleton POC
- **Work:** build the narrowest slice that exercises every layer: auth, the governed flow, the UI,
  the audit trail. Wire the seams to local or mock implementations.
- **Artifact:** a **running prototype** anyone can launch with documented steps.
- **Gate:** the demo proves the architecture hangs together. Stakeholders see where we started.

### 6. Harden and scale
- **Work:** address the deferred runway in priority order: observability (tracing, metrics),
  resilience (outbox, retries), and the scale path (partition then shard on the tenant key).
- **Artifacts:** updated ADRs, runbooks, SLOs, dashboards.
- **Gate:** non-functional targets met under load; security review passed.

## Why a mockup before the POC

A working prototype is the most persuasive artifact we produce, but it is also the most expensive to
change. We put a **wireframe and an API contract in front of stakeholders first** because:

- it aligns product, engineering, and the sponsor on the *experience* in an afternoon, not a sprint;
- it lets us settle the API contract while it is still free to move, before clients depend on it;
- it makes the POC a confirmation of an agreed design rather than a negotiation in code.

The mockup does not replace the prototype. It de-risks it. The sequence is: wireframe to align,
contract to commit, then the walking skeleton to prove.

## Traceability

Strategy to running system is traceable through the artifacts:

```
Strategy / problem brief
  -> Solution design + ADRs            (why this shape)
    -> Wireframes + API contract       (what it looks and behaves like)
      -> Delivery plan: epics, tickets (how we build it, in what order)
        -> Walking-skeleton POC        (proof it works end to end)
          -> Harden + scale            (production readiness)
```

Every layer points back to the one above it, so any engineer or executive can ask "why does this
exist" and follow the trail to a recorded decision.
