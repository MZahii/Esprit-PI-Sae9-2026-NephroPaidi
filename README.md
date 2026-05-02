# NephroPaidi Dockerized Stack

This repository is now structured for full containerized startup with strict priority order:

1. Eureka + Keycloak + Config Server (+ RabbitMQ)
2. API Gateway
3. Remaining microservices + FrontEnd

## 1. Prerequisites

- Docker Desktop installed and running
- Recommended Docker Desktop resources for this stack:
	- CPU: 4 to 6 vCPUs
	- Memory: 8 GB minimum
- Optional: copy `.env.example` to `.env` and adjust secrets/URLs

## 2. Full stack files

- Full orchestration: `docker-compose.full.yml`
- Start script (Windows): `scripts/start-stack.ps1`
- Start script (Linux/macOS): `scripts/start-stack.sh`
- Stop scripts: `scripts/stop-stack.ps1` and `scripts/stop-stack.sh`

## 3. Start the full stack (priority-aware)

From repo root on Windows:

```powershell
./scripts/start-stack.ps1
```

From repo root on Linux/macOS:

```bash
bash ./scripts/start-stack.sh
```

These scripts:

- enforce startup priority (infra -> gateway -> microservices/frontend)
- reduce build/start concurrency with `COMPOSE_PARALLEL_LIMIT=2`
- wait for core endpoints before moving to the next layer

## 4. Stop everything

Windows:

```powershell
./scripts/stop-stack.ps1
```

Linux/macOS:

```bash
bash ./scripts/stop-stack.sh
```

## 5. Manual compose commands (if needed)

Start everything directly:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build
```

Check status:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml ps
```

Stop and cleanup:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml down --remove-orphans
```

## 6. CPU freeze mitigation (important)

If your machine freezes during full builds, use these safeguards:

- Start with the scripts instead of one-shot `up --build`.
- Keep `COMPOSE_PARALLEL_LIMIT=2` (already set in scripts).
- Use the service CPU/memory limits already defined in `docker-compose.full.yml`.
- Avoid rebuilding all services repeatedly; rebuild only changed services:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build api-gateway
```

- If needed, temporarily start only core services first:

```powershell
docker compose -p nephropaidi -f docker-compose.full.yml up -d --build eureka keycloak rabbitmq config-server
```

## 7. Service URLs

- FrontEnd: http://localhost:4200
- API Gateway: http://localhost:8083
- Eureka: http://localhost:8761
- Config Server: http://localhost:8888
- Keycloak: http://localhost:8080
- RabbitMQ Management: http://localhost:15672

## 8. Testing after startup

Frontend tests:

```powershell
cd FrontEnd
npm run test -- --watch=false
```

Backend tests (example):

```powershell
cd BackEnd/microservices/communication-service
./mvnw test
```
