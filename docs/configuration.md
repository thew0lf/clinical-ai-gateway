# Configuration and Security

Every governance behavior of the control plane is externalized configuration with a safe default. Changing where autonomy applies, which model backend runs, or whether budgets enforce is a deployment change, never a code change.

## Environment variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `MODEL_PROVIDER` | `mock` | Model backend behind the provider seam. `mock` for local and tests, `remote` for a real backend. |
| `USAGE_MODE` | `monitor` | `monitor` records usage without blocking. `enforce` applies a hard budget and rejects over it with 429. |
| `USAGE_RPM` | `600` | Per tenant request budget used for the soft warning and for enforce mode. |
| `REVIEW_REQUIRED_ASSISTANTS` | empty | Comma separated assistant types that require human review, for example `CLINICAL,BILLING`. Empty means autonomy. |

Defaults live in `src/main/resources/application.yml` under a typed `app.*` namespace. Two behaviors are deliberately not configurable: PHI redaction and audit publishing are always on, since they are data protection and compliance concerns rather than limits on what the AI can do.

## Authentication and authorization

The gateway is a stateless OAuth2 resource server. Every `/api` request requires a JWT carrying:

- a `tenant_id` claim, applied to every repository query so a tenant can never address another tenant's data
- scope claims gating each operation independently: `suggestion:create`, `suggestion:approve` (approve and reject), and `suggestion:execute`

Only actuator health and info, the OpenAPI UI, and `/dev/**` (dev profile only) are unauthenticated.

## Security note on the bundled keys

The RSA key pair under `src/main/resources/keys` is a throwaway key used only to sign and verify local development and test tokens. It must never be used outside local development. In a real deployment the resource server validates tokens from a real identity provider, and secrets come from a managed secret store.

## Continuous integration

The CI pipeline runs four jobs on every push: the Gradle build with the Testcontainers backed test suite, a Spotless formatting check, a Trivy scan for vulnerabilities, secrets, and misconfigurations, and the Docker image build.
