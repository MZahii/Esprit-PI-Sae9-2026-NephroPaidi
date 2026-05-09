#!/usr/bin/env bash
echo "Starting infra for local dev..."
pushd BackEnd || exit 1
docker compose -f docker-compose.infra.yml up -d
popd
echo "Infrastructure started. Start services from your IDE or run mvnw in microservice folders as needed."
