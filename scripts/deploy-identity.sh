
#!/usr/bin/env bash
set -euo pipefail

cd /opt/project5

COMPOSE_FILE="docker-compose-vps.yml"
IMAGE_REPO="ghcr.io/bebongcute19-gif/project5-identity-service"
HEALTH_URL="http://127.0.0.1:8081/actuator/health"
LKG_FILE="identity-last-known-good.txt"

: "${IMAGE_TAG:?IMAGE_TAG is required}"

test -f .env
test -f scripts/init-postgres-dbs.sql

NEW_IMAGE="$IMAGE_REPO:$IMAGE_TAG"

# determine container name if exists
CONTAINER=""
CONTAINER_CANDIDATES=("project5-identity-service-1" "project5_identity-service_1" "project5-identity-service")
for c in "${CONTAINER_CANDIDATES[@]}"; do
  if docker inspect "$c" >/dev/null 2>&1; then
    CONTAINER="$c"
    break
  fi
done

PREVIOUS_IMAGE=""
if [[ -n "${CONTAINER}" ]]; then
  PREVIOUS_IMAGE=$(docker inspect "$CONTAINER" --format '{{.Config.Image}}' 2>/dev/null || true)
fi

PREVIOUS_TAG="${PREVIOUS_IMAGE##*:}"

echo "Previous image: $PREVIOUS_IMAGE"
echo "New image: $NEW_IMAGE"

check_health() {
  local expected_image="$1"
  local actual_image
  local running
  local response

  for i in $(seq 1 18); do
    actual_image=$(docker inspect "$CONTAINER" \
      --format '{{.Config.Image}}' 2>/dev/null || true)

    running=$(docker inspect "$CONTAINER" \
      --format '{{.State.Running}}' 2>/dev/null || true)

    if [[ "$actual_image" == "$expected_image" &&
          "$running" == "true" ]]; then

      response=$(curl -fsS "$HEALTH_URL" 2>/dev/null || true)

      if [[ "$response" == *'"status":"UP"'* ]]; then
        return 0
      fi
    fi

    sleep 5
  done

  return 1
}

rollback() {
  echo "Starting rollback to $PREVIOUS_IMAGE"

  if [[ -n "$PREVIOUS_TAG" ]]; then
    rollback_tag="$PREVIOUS_TAG"
    echo "Attempting rollback to tag: $rollback_tag"
    IMAGE_TAG="$rollback_tag" docker compose -f "$COMPOSE_FILE" up -d --no-deps identity-service
    # refresh container name
    for c in "${CONTAINER_CANDIDATES[@]}"; do
      if docker inspect "$c" >/dev/null 2>&1; then
        CONTAINER="$c"
        break
      fi
    done
    if check_health "$PREVIOUS_IMAGE"; then
      echo "Rollback successful"
      return 0
    fi
  fi

  echo "Rollback failed - manual intervention required"
  return 1
}

export IMAGE_TAG

docker compose -f "$COMPOSE_FILE" config --quiet

if ! docker compose -f "$COMPOSE_FILE" pull identity-service; then
  echo "Image pull failed. Existing container preserved."
  exit 1
fi

if docker compose -f "$COMPOSE_FILE" up -d \
  --no-deps identity-service; then
  # determine container name after start
  for c in "${CONTAINER_CANDIDATES[@]}"; do
    if docker inspect "$c" >/dev/null 2>&1; then
      CONTAINER="$c"
      break
    fi
  done
  if check_health "$NEW_IMAGE"; then
    echo "$NEW_IMAGE" > "$LKG_FILE"
    echo "Deployment successful: $NEW_IMAGE"
  else
    echo "Deployment failed. Attempting rollback."
    if ! rollback; then
      echo "Rollback failed"
      exit 1
    fi
    exit 1
  fi
else
  echo "Docker compose up failed for identity-service"
  exit 1
fi

docker compose -f "$COMPOSE_FILE" ps
