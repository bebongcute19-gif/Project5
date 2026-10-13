#!/usr/bin/env bash
set -euo pipefail

cd /opt/project5
COMPOSE_FILE="docker-compose-vps.yml"
LKG_DIR=".lkg"
mkdir -p "$LKG_DIR"

: "${IMAGE_TAG:?IMAGE_TAG is required}"

# Readiness configuration (seconds)
READINESS_RETRIES=${READINESS_RETRIES:-18}
READINESS_INTERVAL=${READINESS_INTERVAL:-5}

# identity is deployed separately to preserve existing rollback behavior
services=(product-service order-service payment-service notification-service api-gateway)
health_urls=("http://127.0.0.1:8082/actuator/health" "http://127.0.0.1:8083/actuator/health" "http://127.0.0.1:8084/actuator/health" "http://127.0.0.1:8085/actuator/health" "http://127.0.0.1:8080/actuator/health")

pull_all() {
  echo "Pulling images"
  if ! docker compose -f "$COMPOSE_FILE" pull ${services[*]}; then
    echo "Image pull failed"
    exit 1
  fi
}

# Determine container name (try common variants)
get_container_name() {
  local svc="$1"
  local candidates=("project5-${svc}-1" "project5_${svc}_1")
  for c in "${candidates[@]}"; do
    if docker inspect "$c" >/dev/null 2>&1; then
      echo "$c"
      return 0
    fi
  done
  # fallback to compose-generated name using docker compose ps
  name=$(docker compose -f "$COMPOSE_FILE" ps --format "{{.Name}} {{.Service}}" | awk -v s="$svc" '$2==s {print $1; exit}')
  if [[ -n "$name" ]]; then
    echo "$name"
    return 0
  fi
  # no container found yet
  echo ""
}

check_health() {
  local expected_image="$1"
  local container_name="$2"
  local url="$3"
  local svc="$4"

  local start_ts=$(date +%s)
  for i in $(seq 1 "$READINESS_RETRIES"); do
    # try to discover container if not provided
    if [[ -z "$container_name" && -n "$svc" ]]; then
      container_name=$(get_container_name "$svc")
    fi

    actual_image=""
    running=""
    if [[ -n "$container_name" ]]; then
      actual_image=$(docker inspect "$container_name" --format '{{.Config.Image}}' 2>/dev/null || true)
      running=$(docker inspect "$container_name" --format '{{.State.Running}}' 2>/dev/null || true)
    fi

    http_code=""
    body=""
    if curl -s -o /tmp/health_body.$$ -w "%{http_code}" "$url" >/tmp/http_code.$$ 2>/dev/null; then
      http_code=$(cat /tmp/http_code.$$)
      body=$(cat /tmp/health_body.$$ 2>/dev/null || true)
      rm -f /tmp/health_body.$$ /tmp/http_code.$$
    else
      http_code=""
    fi

    elapsed=$(( $(date +%s) - start_ts ))
    echo "check_health attempt $i: container='$container_name' image='$actual_image' running='$running' http_code='$http_code' elapsed='${elapsed}s'" >&2

    if [[ -n "$actual_image" && "$actual_image" != "$expected_image" ]]; then
      echo "Expected image '$expected_image' but container is running '$actual_image'" >&2
    fi

    if [[ "$http_code" == "200" ]]; then
      if [[ "$body" == *'"status":"UP"'* || "$body" == *'UP'* ]]; then
        if [[ -n "$actual_image" && "$actual_image" == "$expected_image" && "$running" == "true" ]]; then
          return 0
        else
          echo "Health OK but image/state mismatch: container='$container_name' image='$actual_image' running='$running'" >&2
        fi
      fi
    fi

    sleep "$READINESS_INTERVAL"
  done

  return 1
}

deploy_service() {
  local svc="$1"
  local url="$2"
  local repo="ghcr.io/bebongcute19-gif/project5-${svc}"

  echo "Deploying $svc"
  # determine existing container name and image
  container_name=$(get_container_name "$svc")
  PREVIOUS_IMAGE=""
  if [[ -n "$container_name" ]]; then
    PREVIOUS_IMAGE=$(docker inspect "$container_name" --format '{{.Config.Image}}' 2>/dev/null || true)
  fi
  PREVIOUS_TAG="${PREVIOUS_IMAGE##*:}"

  NEW_IMAGE="${repo}:${IMAGE_TAG}"

  if ! docker compose -f "$COMPOSE_FILE" pull "$svc"; then
    echo "Failed to pull $svc"
    return 1
  fi

  if docker compose -f "$COMPOSE_FILE" up -d --no-deps "$svc"; then
    # refresh container name after start
    container_name=$(get_container_name "$svc")
    if check_health "$NEW_IMAGE" "$container_name" "$url" "$svc"; then
      echo "$svc deployed: $NEW_IMAGE"
      echo "$NEW_IMAGE" > "$LKG_DIR/${svc}.lkg"
      return 0
    fi
  fi

  echo "Deployment failed for $svc, attempting rollback"
  if [[ -n "$PREVIOUS_TAG" ]]; then
    rollback_tag="$PREVIOUS_TAG"
    echo "Attempting rollback to tag: $rollback_tag"
    # pull and start previous image without changing global IMAGE_TAG
    IMAGE_TAG="$rollback_tag" docker compose -f "$COMPOSE_FILE" pull "$svc"
    IMAGE_TAG="$rollback_tag" docker compose -f "$COMPOSE_FILE" up -d --no-deps "$svc"
    # refresh container name
    container_name=$(get_container_name "$svc")
    expected_prev_image="${PREVIOUS_IMAGE:-ghcr.io/bebongcute19-gif/project5-${svc}:$rollback_tag}"
    if check_health "$expected_prev_image" "$container_name" "$url" "$svc"; then
      echo "Rollback successful for $svc"
      return 1
    fi
  else
    echo "No previous image available for $svc; rollback unavailable"
  fi

  echo "Rollback failed for $svc"
  return 1
}

# Validate files
docker compose -f "$COMPOSE_FILE" config --quiet

# Pull DB, redis and rabbitmq first
docker compose -f "$COMPOSE_FILE" pull postgresql-db redis rabbitmq

docker compose -f "$COMPOSE_FILE" up -d postgresql-db redis rabbitmq

# Ensure postgres and redis containers become healthy before proceeding
# Wait for postgres
pg_container=$(get_container_name "postgresql-db")
if [[ -z "$pg_container" ]]; then
  echo "Postgres container not found after start"
  exit 1
fi
for i in $(seq 1 18); do
  status=$(docker inspect "$pg_container" --format '{{.State.Health.Status}}' 2>/dev/null || true)
  if [[ "$status" == "healthy" ]]; then
    echo "Postgres is healthy"
    break
  fi
  sleep 5
  if [[ $i -eq 18 ]]; then
    echo "Postgres failed to become healthy"
    exit 1
  fi
done

# Wait for redis
redis_container=$(get_container_name "redis")
if [[ -z "$redis_container" ]]; then
  echo "Redis container not found after start"
  exit 1
fi
for i in $(seq 1 18); do
  status=$(docker inspect "$redis_container" --format '{{.State.Health.Status}}' 2>/dev/null || true)
  if [[ "$status" == "healthy" ]]; then
    echo "Redis is healthy"
    break
  fi
  sleep 5
  if [[ $i -eq 18 ]]; then
    echo "Redis failed to become healthy"
    exit 1
  fi
done

# Wait for rabbitmq
rabbit_container=$(get_container_name "rabbitmq")
if [[ -z "$rabbit_container" ]]; then
  echo "RabbitMQ container not found after start"
  exit 1
fi
for i in $(seq 1 18); do
  status=$(docker inspect "$rabbit_container" --format '{{.State.Health.Status}}' 2>/dev/null || true)
  if [[ "$status" == "healthy" ]]; then
    echo "RabbitMQ is healthy"
    break
  fi
  sleep 5
  if [[ $i -eq 18 ]]; then
    echo "RabbitMQ failed to become healthy"
    exit 1
  fi
done

# Deploy services one by one
for idx in "${!services[@]}"; do
  svc=${services[$idx]}
  url=${health_urls[$idx]}
  if ! deploy_service "$svc" "$url"; then
    echo "Deployment aborted due to failure on $svc"
    exit 1
  fi
done

# Final status
docker compose -f "$COMPOSE_FILE" ps

echo "All services deployed and healthy"
