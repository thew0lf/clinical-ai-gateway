# Reviewer Console: Wireframes and API Contract

Status: Draft for review
Purpose: Align stakeholders on the experience and the API surface **before** the prototype is built.

These are low-fidelity wireframes on purpose. They exist to settle layout, flow, and the data each
screen needs, while those are still cheap to change. The visual design is not the point yet.

## Personas and roles

| Persona | Role | Scopes | Sees |
| --- | --- | --- | --- |
| Clinician / Requester | `CLINICIAN` | `suggestion:create` | Create, own queue |
| Reviewer | `REVIEWER` | `suggestion:approve` | Pending queue, approve / reject |
| Admin | `ADMIN` | all + read | Everything, audit, usage |

Auth is OIDC in production (Cognito / Auth0 / Keycloak). For the POC the gateway mints its own
verified JWT so the demo is self-contained, using the same resource-server validation path a real
IdP would use.

## Screen 1: Login

```
+--------------------------------------------------------------+
|                  Clinical AI Control Plane                   |
|                                                              |
|              Sign in to the Reviewer Console                 |
|                                                              |
|   Continue as:                                               |
|     ( ) Dr. Chen        - Clinician   (create suggestions)   |
|     (o) Morgan Reyes    - Reviewer    (approve / reject)     |
|     ( ) Alex Lee        - Admin       (full access)          |
|                                                              |
|                  [  Sign in with SSO  ]                      |
|                                                              |
|   OIDC in production. Demo issues a gateway-signed JWT.      |
+--------------------------------------------------------------+
```

## Screen 2: Suggestion queue (Reviewer landing)

```
+--------------------------------------------------------------+
| Control Plane    Queue  Create  Audit  Usage     Morgan (REV)|
+--------------------------------------------------------------+
| Tenant: tenant-alpha                  Filter: [ PENDING v ]   |
+--------------------------------------------------------------+
| Status   Assistant   Encounter  Tokens  Created      Action  |
| PENDING  BILLING     enc-1001   142     2m ago        [open] |
| PENDING  CLINICAL    enc-1042   310     11m ago       [open] |
| APPROVED FRONT_OFFICE enc-1008  88      1h ago  policy:auto  |
| REJECTED PATIENT     enc-1102   201     2h ago    m.reyes    |
+--------------------------------------------------------------+
```

States: `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`. The `decidedBy` column shows `policy:auto`
for autonomous approvals and the reviewer id for human decisions.

## Screen 3: Suggestion detail and decision

```
+--------------------------------------------------------------+
| < Back to queue                            Suggestion #a3f9  |
+--------------------------------------------------------------+
| Status: PENDING       Assistant: BILLING   Tenant: alpha     |
| Requested by: dr.chen           Created: 2026-06-24 14:02    |
+--------------------------------------------------------------+
| Redacted prompt (what left the trust boundary):              |
|   "Patient [REDACTED_SSN] seen for acne follow up."          |
|                                                              |
| Model output:                                                |
|   "Suggested CPT 99213 for established patient visit..."     |
|   provider: mock      tokens: 142                            |
+--------------------------------------------------------------+
| Grounding / provenance:                                      |
|   ENCOUNTER  enc-1001   "visit note"                         |
+--------------------------------------------------------------+
| Decision:   [ Approve ]   [ Reject ]                         |
|   Reject reason: [______________________________]           |
+--------------------------------------------------------------+
| Audit trail (this suggestion):                               |
|   14:02  SUGGESTION_CREATED   dr.chen   reviewRequired=true  |
|   --                                                          |
+--------------------------------------------------------------+
```

The redaction is shown explicitly because it is the most important governance story: PHI is removed
at the gateway before any model call, once, for every assistant.

## Screen 4: Create suggestion (shows redaction live)

```
+--------------------------------------------------------------+
| Create a governed suggestion                                 |
+--------------------------------------------------------------+
| Assistant type: [ BILLING v ]   Encounter: [ enc-1001 ]      |
| Input text:                                                  |
|  [ Patient SSN 123-45-6789 seen for acne follow up.       ]  |
|                                                              |
| Preview of redacted prompt that will leave the boundary:     |
|  "Patient [REDACTED_SSN] seen for acne follow up."           |
|                                                              |
| Grounding reference:                                         |
|  type [ ENCOUNTER v ]  id [ enc-1001 ]  desc [ visit note ]  |
|                                                              |
|                              [ Submit ]                      |
+--------------------------------------------------------------+
```

## Screen 5: Audit trail (tenant feed)

```
+--------------------------------------------------------------+
| Audit trail - tenant-alpha                  Last [ 50 v ]    |
+--------------------------------------------------------------+
| Time     Event                  Suggestion  Actor            |
| 14:09    SUGGESTION_APPROVED     #a3f9       m.reyes          |
| 14:02    SUGGESTION_CREATED      #a3f9       dr.chen          |
| 13:51    SUGGESTION_AUTO_APPROVED #b1c4      policy:auto      |
| 13:51    SUGGESTION_CREATED      #b1c4       dr.chen          |
+--------------------------------------------------------------+
| Read model populated by the Kafka audit consumer.            |
```

## Screen 6: Usage and cost dashboard

```
+--------------------------------------------------------------+
| Usage and cost - tenant-alpha          Mode: MONITOR         |
+--------------------------------------------------------------+
|  Requests this minute      Tokens today      Soft budget     |
|       37 / 600                 4,210            600 rpm       |
|  [#####.................]                                     |
|                                                              |
|  Posture: usage is reported, not enforced. ENFORCE mode      |
|  applies a hard budget per tenant when policy requires it.   |
+--------------------------------------------------------------+
```

## API contract this implies

| Method | Path | Scope | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | public (dev) | Exchange demo credentials for a gateway JWT |
| POST | `/api/v1/suggestions` | `suggestion:create` | Create a governed suggestion |
| GET | `/api/v1/suggestions` | authenticated | List tenant suggestions |
| GET | `/api/v1/suggestions/{id}` | authenticated | Detail + provenance |
| POST | `/api/v1/suggestions/{id}/approve` | `suggestion:approve` | Approve |
| POST | `/api/v1/suggestions/{id}/reject` | `suggestion:approve` | Reject with reason |
| POST | `/api/v1/suggestions/{id}/execute` | `suggestion:execute` | Mark executed |
| GET | `/api/v1/suggestions/{id}/audit` | authenticated | Per-suggestion trail |
| GET | `/api/v1/audit` | authenticated | Tenant audit feed |
| GET | `/api/v1/usage` | authenticated | Usage snapshot |

The create, decision, and execute routes already exist. `login`, the two audit reads, and `usage`
are the additions this console requires; they are tracked in the delivery plan.
