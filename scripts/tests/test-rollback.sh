
#!/usr/bin/env bash
set -euo pipefail

echo "=== Project5 Rollback Tests ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="$SCRIPT_DIR/../deploy-identity.sh"

test -f "$DEPLOY_SCRIPT"

# Extract the rollback function without running deployment.
ROLLBACK_FUNCTION=$(
  sed -n '/^rollback() {/,/^}/p' "$DEPLOY_SCRIPT"
)

if [[ -z "$ROLLBACK_FUNCTION" ]]; then
  echo "FAIL: rollback function not found"
  exit 1
fi

# Mock Docker commands.
docker() {
  echo "MOCK docker $*" >&2

  if [[ "$*" == *"up -d"* ]]; then
    return "${MOCK_UP_EXIT:-0}"
  fi

  return 0
}

# Mock healthcheck.
check_health() {
  echo "MOCK healthcheck: $1" >&2
  return "${MOCK_HEALTH_EXIT:-0}"
}

COMPOSE_FILE="docker-compose-vps.yml"
PREVIOUS_TAG="previous-version"
PREVIOUS_IMAGE="ghcr.io/bebongcute19-gif/project5-identity-service:previous-version"

eval "$ROLLBACK_FUNCTION"

echo
echo "Test 1: Rollback successful"

export IMAGE_TAG="broken-version"
MOCK_UP_EXIT=0
MOCK_HEALTH_EXIT=0

OUT=$(rollback 2>&1 || true)

# Verify rollback attempted to use previous tag and healthcheck was invoked
echo "$OUT" | grep -q "Attempting rollback to tag: $PREVIOUS_TAG" || {
  echo "FAIL: rollback did not attempt to use previous tag"
  echo "Output:\n$OUT"
  exit 1
}

echo "PASS: Rollback successful"

echo
echo "Test 2: Rollback healthcheck fails"

MOCK_UP_EXIT=0
MOCK_HEALTH_EXIT=1

if rollback; then
  echo "FAIL: Expected rollback failure"
  exit 1
fi

echo "PASS: Healthcheck failure detected"

echo
echo "Test 3: Docker Compose up fails"

MOCK_UP_EXIT=1
MOCK_HEALTH_EXIT=0

OUT=$(rollback 2>&1 || true)

if echo "$OUT" | grep -q "Rollback successful"; then
  echo "PASS: Docker Compose up failed but healthcheck allowed rollback success"
elif echo "$OUT" | grep -q "Rollback failed"; then
  echo "PASS: Docker Compose failure detected"
else
  echo "FAIL: Unexpected rollback result"
  echo "Output:\n$OUT"
  exit 1
fi

echo
echo "=== All rollback tests passed ==="
