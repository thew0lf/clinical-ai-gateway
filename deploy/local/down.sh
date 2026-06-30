#!/usr/bin/env bash
#
# Tear down the local Clinical AI Control Plane deployment.
# Pinned to the "minikube" context so it can never touch a remote cluster.
# Usage: deploy/local/down.sh
#
set -euo pipefail

NS="clinical-ai"
RELEASE="gw"
KCTX="minikube"

helm --kube-context "${KCTX}" uninstall "${RELEASE}" --namespace "${NS}" 2>/dev/null || true
kubectl --context "${KCTX}" delete namespace "${NS}" --ignore-not-found
echo "==> Down. Namespace ${NS} removed from minikube. (minikube itself is left running.)"
