#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.full.yml}"
PROJECT_NAME="${2:-nephropaidi}"

docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans
echo "Stack stopped."
