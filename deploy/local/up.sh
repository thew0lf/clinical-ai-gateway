#!/usr/bin/env bash
#
# Bring the Clinical AI Control Plane up on a local minikube cluster.
# Builds the application image as clinical-ai-gateway:0.1.0, deploys Postgres, Redis, and Kafka, then
# installs the gateway via Helm. Idempotent: safe to re-run.
#
# Every kubectl and helm command is pinned to the "minikube" context so this script can never
# touch a remote cluster, regardless of the current kubectl context.
#
# Usage: deploy/local/up.sh
#
set -euo pipefail

# Local-only image name (not a public Docker Hub repo) to avoid IfNotPresent pulling a same-named
# public image instead of the one we build and load.
IMAGE="clinical-ai-gateway:0.1.0"
NS="clinical-ai"
RELEASE="gw"
KCTX="minikube"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

KC="kubectl --context ${KCTX}"
HM="helm --kube-context ${KCTX}"

# Build on the host Docker first, while the full Docker Desktop memory is available, then load the
# finished image into minikube. This keeps the heavy Gradle build from competing with the running
# cluster for the same constrained memory.
echo "==> Building ${IMAGE} on the host Docker"
docker build -t "${IMAGE}" -f "${ROOT}/docker/Dockerfile" "${ROOT}"

echo "==> Ensuring minikube is running"
# Memory is sized to fit a default Docker Desktop allocation (~4GB). If you have given Docker more,
# you can raise this for headroom. Override with MK_MEMORY / MK_CPUS.
if ! minikube status >/dev/null 2>&1; then
  minikube start --cpus="${MK_CPUS:-3}" --memory="${MK_MEMORY:-3500}"
fi

echo "==> Loading ${IMAGE} into the cluster"
# Load via a tar export. With the docker driver, "minikube image load <name>" can fail to locate a
# host image; loading an explicit archive is deterministic.
TAR="$(mktemp -t clinical-ai-gateway).tar"
docker save "${IMAGE}" -o "${TAR}"
minikube image load "${TAR}"
rm -f "${TAR}"

echo "==> Applying dependency manifests (Postgres, Redis, Kafka)"
${KC} apply -f "${ROOT}/deploy/local/00-namespace.yaml"
${KC} apply -f "${ROOT}/deploy/local/10-postgres.yaml"
${KC} apply -f "${ROOT}/deploy/local/20-redis.yaml"
${KC} apply -f "${ROOT}/deploy/local/30-kafka.yaml"

echo "==> Waiting for dependencies to become ready"
${KC} -n "${NS}" rollout status deployment/postgres --timeout=120s
${KC} -n "${NS}" rollout status deployment/redis --timeout=120s
${KC} -n "${NS}" rollout status deployment/kafka --timeout=180s

echo "==> Installing the gateway via Helm"
${HM} upgrade --install "${RELEASE}" "${ROOT}/helm/clinical-ai-gateway" \
  --namespace "${NS}" \
  -f "${ROOT}/deploy/local/values-local.yaml"

echo "==> Waiting for the gateway to become ready"
${KC} -n "${NS}" rollout status deployment/${RELEASE}-clinical-ai-gateway --timeout=180s

cat <<EOF

==> Up. The control plane is running in namespace ${NS} on the minikube cluster.

Port-forward the API:
  kubectl --context ${KCTX} -n ${NS} port-forward svc/${RELEASE}-clinical-ai-gateway 8080:80

Then, in another shell:
  curl -s localhost:8080/actuator/health
  TOKEN=\$(curl -s "localhost:8080/dev/token" | sed -n 's/.*"access_token":"\\([^"]*\\)".*/\\1/p')
  curl -s -X POST localhost:8080/api/v1/suggestions \\
    -H "Authorization: Bearer \$TOKEN" -H "Content-Type: application/json" \\
    -d '{"encounterId":"enc-1001","assistantType":"BILLING","inputText":"Patient SSN 123-45-6789 seen for acne follow up."}'
  curl -s localhost:8080/api/v1/usage -H "Authorization: Bearer \$TOKEN"
  curl -s localhost:8080/api/v1/audit -H "Authorization: Bearer \$TOKEN"

Tear down with: deploy/local/down.sh
EOF
