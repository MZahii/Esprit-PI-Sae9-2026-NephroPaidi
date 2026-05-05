#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.full.yml}"
PROJECT_NAME="${2:-nephropaidi}"

wait_url() {
  local url="$1"
  local timeout="${2:-180}"
  local start
  start=$(date +%s)

  while true; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "Ready: $url"
      return 0
    fi

    if [ $(( $(date +%s) - start )) -ge "$timeout" ]; then
      echo "Timeout while waiting for $url" >&2
      return 1
    fi

    sleep 5
  done
}

export COMPOSE_PARALLEL_LIMIT=2
echo "COMPOSE_PARALLEL_LIMIT=$COMPOSE_PARALLEL_LIMIT (CPU guard)"

echo "Step 1/3: starting core infra (eureka, keycloak, rabbitmq, config-server)..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build eureka keycloak rabbitmq config-server
wait_url "http://localhost:8761/actuator/health" 300
wait_url "http://localhost:8888/actuator/health" 300
wait_url "http://localhost:8080/realms/master/.well-known/openid-configuration" 300

echo "Step 2/4: starting backend microservices (without gateway/frontend)..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build user-service administration-service communication-service clinical-service ops-service core-ops-service pharmacy-service procedure-service
wait_url "http://localhost:8090/actuator/health" 300
wait_url "http://localhost:8087/actuator/health" 300

echo "Step 3/4: starting api-gateway after services are discoverable..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build api-gateway
wait_url "http://localhost:8083/actuator/health" 240

echo "Step 4/4: starting frontend..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build frontend

echo "Done. Stack is starting in priority order."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps
