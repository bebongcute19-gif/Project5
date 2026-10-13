#!/usr/bin/env bash
set -euo pipefail

# Regression test: ensure API Gateway host-side health check uses port 80
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." >/dev/null 2>&1 && pwd)"

# Check deploy-all.sh health_urls
if ! grep -q "127.0.0.1:80/actuator/health" "$ROOT_DIR/scripts/deploy-all.sh"; then
  echo "FAIL: deploy-all.sh does not use port 80 for api-gateway health check"
  exit 1
fi

# Check docker-compose-vps.yml port mapping for api-gateway
if ! grep -q "0.0.0.0:80:8080" "$ROOT_DIR/docker-compose-vps.yml"; then
  echo "WARN: docker-compose-vps.yml does not explicitly map host port 80 for api-gateway"
  echo "This is acceptable if API Gateway is intended to bind host port 80 via compose."
else
  echo "docker-compose-vps.yml maps API Gateway to host port 80"
fi

echo "PASS: API Gateway health check host port configured to 80 in deploy script"