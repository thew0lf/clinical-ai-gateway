# Local Kubernetes deployment

Runs the entire Clinical AI Control Plane on a local single-node Kubernetes cluster
([minikube](https://minikube.sigs.k8s.io/)): the gateway plus its Postgres, Redis, and Kafka
dependencies. This is the cloud-native runnable proof of the system, without provisioning any cloud
resources.

For the production topology this models, see `helm/clinical-ai-gateway` (the chart) and `terraform/`
(the EKS and VPC infrastructure as code).

## Prerequisites

- Docker Desktop (with at least ~4GB allocated to it)
- `minikube`, `kubectl`, and `helm` on the PATH

## Up

```
deploy/local/up.sh
```

The script is idempotent and safe to re-run. It:

1. builds the application image as `clinical-ai-gateway:0.1.0` on the host Docker;
2. starts minikube (sized to fit a default Docker Desktop allocation) and loads the image into it;
3. applies the dependency manifests (Postgres, Redis, Kafka) into the `clinical-ai` namespace;
4. installs the gateway via Helm using `values-local.yaml`;
5. waits for every rollout to become ready.

Every `kubectl` and `helm` command is pinned to the `minikube` context, so the script can never
touch a remote cluster regardless of your current context.

### Resource overrides

If you have given Docker more memory, raise the cluster size for headroom:

```
MK_MEMORY=6144 MK_CPUS=4 deploy/local/up.sh
```

## Exercise the API

Port-forward the service:

```
kubectl --context minikube -n clinical-ai port-forward svc/gw-clinical-ai-gateway 8080:80
```

Then, in another shell (the dev profile is active in the cluster, so `/dev/token` is available):

```
curl -s localhost:8080/actuator/health
TOKEN=$(curl -s "localhost:8080/dev/token" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

curl -s -X POST localhost:8080/api/v1/suggestions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"encounterId":"enc-1001","assistantType":"BILLING","inputText":"Patient SSN 123-45-6789 seen for acne follow up."}'

curl -s localhost:8080/api/v1/usage  -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/v1/audit  -H "Authorization: Bearer $TOKEN"
```

## Inspect

```
kubectl --context minikube -n clinical-ai get pods
kubectl --context minikube -n clinical-ai logs deploy/gw-clinical-ai-gateway
```

## Down

```
deploy/local/down.sh
```

Removes the release and the namespace from minikube. minikube itself is left running; stop it with
`minikube stop` when you are done.

## Notes and boundaries

- The dependencies use ephemeral storage (`emptyDir`); data does not survive a pod restart. This is
  intentional for a prototype. A real cluster uses a managed database and managed Kafka (for example
  RDS/Aurora and MSK), or StatefulSets with persistent volumes.
- The dev profile activates the local token endpoint so the API can be exercised without an external
  identity provider. In a real deployment the resource server validates tokens from a real IdP and
  this endpoint does not exist.
- Secrets here are demo values. A real cluster sources them from a managed secret store, for example
  AWS Secrets Manager via the External Secrets Operator.
