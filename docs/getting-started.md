# Getting Started

How to run the Clinical AI Control Plane locally, exercise the governed flow, run it on a local Kubernetes cluster, and run the tests.

## Prerequisites

- Java 21
- Docker and Docker Compose
- minikube and Helm (only for the local Kubernetes path)

## Run locally

Start the infrastructure (Postgres, Redis, single node Kafka):

```bash
docker compose up -d
```

Run the application with the dev profile, which enables a local token endpoint:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Mint a development token:

```bash
TOKEN=$(curl -s "http://localhost:8080/dev/token" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
```

Create a suggestion:

```bash
curl -s -X POST http://localhost:8080/api/v1/suggestions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "encounterId": "enc-1001",
        "assistantType": "BILLING",
        "inputText": "Patient SSN 123-45-6789 seen for acne follow up.",
        "groundingReferences": [
          {"sourceType": "ENCOUNTER", "sourceId": "enc-1001", "description": "visit note"}
        ]
      }'
```

With the default configuration the suggestion is auto approved, since no assistant requires review. The response shows an APPROVED status and a decidedBy of `policy:auto`. Note that the SSN in the input is redacted before the model call; the persisted suggestion carries the redacted text.

### The human review path

Start the application with review required for an assistant type:

```bash
REVIEW_REQUIRED_ASSISTANTS=BILLING SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Now a BILLING suggestion is created in PENDING state and can be approved:

```bash
curl -s -X POST http://localhost:8080/api/v1/suggestions/<id>/approve \
  -H "Authorization: Bearer $TOKEN"
```

### Explore the API

The full contract is served by the running application at `http://localhost:8080/swagger-ui.html`. Usage counters are at `GET /api/v1/usage` and the audit trail at `GET /api/v1/audit`.

## Run on Kubernetes (local)

The whole control plane, including its Postgres, Redis, and Kafka dependencies, runs on a local single node Kubernetes cluster ([minikube](https://minikube.sigs.k8s.io/)). This is the cloud native runnable proof of the system without provisioning any cloud resources. It uses the same Helm chart under `helm/clinical-ai-gateway` that targets EKS in production.

```bash
deploy/local/up.sh
```

The script builds the application image as `clinical-ai-gateway:0.1.0`, starts minikube, loads the image, deploys the dependencies, and installs the gateway via Helm. Every command is pinned to the `minikube` context, so it cannot touch a remote cluster. Full instructions, including how to exercise the API once it is up, are in [`deploy/local/README.md`](../deploy/local/README.md). Tear down with `deploy/local/down.sh`.

## Tests

```bash
./gradlew test
```

The unit tests cover redaction, usage metering in both monitor and enforce modes, and the orchestration logic including the autonomous and review required paths. The integration test starts Postgres, Kafka, and Redis with Testcontainers, drives the create and approve flow over HTTP with a signed token, and waits for the audit consumer to persist the trail. Running the tests requires a Docker daemon.

Run a single test class:

```bash
./gradlew test --tests SuggestionServiceTest
```

## Formatting

Formatting is google-java-format via Spotless, checked as a separate continuous integration job so a fresh clone builds green. Apply formatting before committing:

```bash
./gradlew spotlessApply
```
