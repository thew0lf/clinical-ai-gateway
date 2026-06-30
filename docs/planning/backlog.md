# Delivery Plan and Backlog: Reviewer Console + Cloud Readiness

Status: Active
Owner: Software Architecture
Related: `../architecture/process.md`, `../design/wireframes.md`, `../adr/`

This is the decomposition of the Reviewer Console initiative and the surrounding cloud-readiness
work into epics and vertically sliced tickets. Each ticket has acceptance criteria. Sequencing puts
the riskiest assumption first: that the governed flow holds together end to end across auth, the
API, the UI, and the audit trail.

Status legend: `DONE` shipped in this repo, `IN PROGRESS`, `TODO`.

## Milestones

- **M1 Walking skeleton** (Epics A–C): a runnable prototype, login through approve, with the audit
  trail and usage visible. This is "where we started" for the stakeholder demo.
- **M2 Cloud-native runnable** (Epics D–E): one command to run locally; a clean container; a
  Kubernetes deploy that comes up on a local cluster.
- **M3 Production posture** (Epics F–H): IaC validated, observability, and the resilience runway.
- **M4 Repo polish** (Epic I): documentation and GitHub readiness.

---

## Epic A: Governance read APIs

The console needs to read usage and the audit trail. The write/decision routes already existed.

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| A1 | Usage read methods on the metering service | DONE | Service exposes current request count, tokens today, budget, and mode without blocking |
| A2 | `GET /api/v1/usage` | DONE | Returns a tenant-scoped usage snapshot; tenant read from JWT |
| A3 | Audit repo queries (per suggestion, tenant feed) | DONE | Both queries tenant-scoped; feed paged and newest-first |
| A4 | `GET /api/v1/suggestions/{id}/audit` and `GET /api/v1/audit` | DONE | Return the trail / feed as DTOs; never leak another tenant's events |
| A5 | Unit tests for usage read and audit read | TODO | Cover monitor/enforce read paths and tenant isolation |

## Epic B: Demo authentication

OIDC in production; a self-contained gateway-issued JWT for the prototype.

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| B1 | `POST /api/auth/login` with Clinician / Reviewer / Admin personas | TODO | Returns a signed JWT with tenant + role scopes via the existing minter; dev profile only |
| B2 | CORS for the SPA dev origin | TODO | SPA at localhost can call the API under the dev profile only |
| B3 | Permit `/api/auth/**`, keep all other `/api/**` authenticated | TODO | Security filter chain updated; production path untouched |
| B4 | Login slice test | TODO | Each persona receives the documented scopes; bad credentials rejected |

## Epic C: Reviewer Console frontend

Next.js + TypeScript + Tailwind, against the contract in the wireframes.

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| C1 | Scaffold app, API client, auth/token context | TODO | App boots; token attached to every request; sign-out clears it |
| C2 | Login screen with persona picker | TODO | Matches wireframe; role drives nav |
| C3 | Suggestion queue with status filters | TODO | Lists tenant suggestions; filter works; cost column shown |
| C4 | Suggestion detail + approve/reject | TODO | Reviewer can decide; reason captured on reject; redacted prompt shown |
| C5 | Create suggestion with live redaction preview | TODO | Submitting creates a suggestion; redaction visible before submit |
| C6 | Audit trail view (per suggestion + tenant feed) | TODO | Renders the read model; newest first |
| C7 | Usage dashboard | TODO | Shows requests/min vs budget and tokens today |

## Epic D: Local runnable infrastructure

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| D1 | Docker Compose for Postgres, Redis, Kafka | DONE | `docker compose up -d` brings up the dependencies |
| D2 | One-command dev run documented | IN PROGRESS | README shows infra up + `bootRun` dev + `web` dev; verified end to end |

## Epic E: Container and Kubernetes

This maps to the JD's "Containers, Kubernetes" and "CI/CD standards" requirements.

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| E1 | Application Dockerfile | DONE | `docker build` produces a runnable image |
| E2 | Helm chart (deployment, service, config, secret, SA) | DONE | Chart exists under `helm/clinical-ai-gateway` |
| E3 | `helm lint` + `helm template` clean | TODO | Both pass; values documented |
| E4 | Local Kubernetes deploy (kind / minikube) | TODO | App + deps come up on a local cluster; health probe green; documented |
| E5 | CI builds and publishes the image | TODO | GitHub Actions builds the image on push; tags by version |

## Epic F: Infrastructure as code (EKS)

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| F1 | Terraform for VPC + EKS | DONE | Modules exist under `terraform/` |
| F2 | `terraform validate` + `fmt` clean in CI | TODO | Both pass without applying; no drift in formatting |
| F3 | Document the apply path and cost caveat | TODO | README notes that apply provisions real AWS resources and is out of scope for the demo |

## Epic G: Observability (SRE/DevOps)

Maps to the JD's "observability (APM, logging)" requirement.

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| G1 | OpenTelemetry tracing across HTTP, JPA, Kafka | TODO | A create-then-approve flow shows one trace spanning the layers |
| G2 | Prometheus metrics via Actuator + Micrometer | TODO | `/actuator/prometheus` exposes request, token, and decision counters |
| G3 | Structured JSON logging with tenant + suggestion correlation | TODO | Every log line carries tenant and suggestion id where applicable |

## Epic H: Resilience runway

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| H1 | Transactional outbox for audit events | TODO | Audit publish no longer shares the write transaction; closes ADR 0003 gap |
| H2 | Scale note: partition then shard on `tenant_id` | TODO | ADR records the partition-first, shard-when-saturated path |

## Epic I: Documentation and GitHub readiness

| ID | Ticket | Status | Acceptance criteria |
| --- | --- | --- | --- |
| I1 | `.gitignore` covers frontend + build artifacts | TODO | `node_modules`, `.next`, build output excluded |
| I2 | README: architecture process + run-everything section | TODO | New reader can run backend + frontend + (optional) local k8s |
| I3 | `web/README.md` | TODO | Frontend setup, scripts, env, and the auth note |
| I4 | Link docs set from the root README | TODO | Process, wireframes, backlog, and ADRs all linked |

---

## Sequencing rationale

1. **A and B first** because the UI cannot exist without its contract and auth. Most are already done.
2. **C** proves the architecture end to end. This is the demo that shows "where we started."
3. **D and E** make it cloud-native runnable, which is the JD's headline requirement, without
   spending money on a real cluster: a local kind/minikube deploy is the credible proof.
4. **F** stays validate-only. We do not apply EKS for a prototype; we show the IaC is sound.
5. **G and H** are the deferred runway that turns "modern stack" into "production posture." They are
   the strongest architecture talking points and are sequenced after the skeleton is proven.
6. **I** keeps the repo presentable for upload.
