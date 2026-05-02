# Swagger Access Guide

This project supports Swagger documentation in two ways:

1. Direct access to each microservice
2. Centralized access via API Gateway

## Direct Microservice Swagger URLs

- User Service: `http://localhost:8090/swagger-ui/index.html`
- Administration Service: `http://localhost:8087/swagger-ui/index.html`
- Clinical Service: `http://localhost:8084/swagger-ui/index.html`
- Communication Service: `http://localhost:8085/swagger-ui/index.html`
- Procedure Service: `http://localhost:8089/swagger-ui/index.html`
- Pharmacy Service: `http://localhost:8088/swagger-ui/index.html`
- Ops Service: `http://localhost:8082/swagger-ui/index.html`
- Core Ops Service (Python): `http://localhost:8086/swagger-ui/index.html`

## Gateway Swagger URLs

- User Service: `http://localhost:8083/swagger/user-service/index.html`
- Administration Service: `http://localhost:8083/swagger/administration-service/index.html`
- Clinical Service: `http://localhost:8083/swagger/clinical-service/index.html`
- Communication Service: `http://localhost:8083/swagger/communication-service/index.html`
- Procedure Service: `http://localhost:8083/swagger/procedure-service/index.html`
- Pharmacy Service: `http://localhost:8083/swagger/pharmacy-service/index.html`
- Ops Service: `http://localhost:8083/swagger/ops-service/index.html`
- Core Ops Service (Python): `http://localhost:8083/swagger/core-ops-service/index.html`

## Gateway OpenAPI JSON URLs

- User Service: `http://localhost:8083/v3/api-docs/user-service`
- Administration Service: `http://localhost:8083/v3/api-docs/administration-service`
- Clinical Service: `http://localhost:8083/v3/api-docs/clinical-service`
- Communication Service: `http://localhost:8083/v3/api-docs/communication-service`
- Procedure Service: `http://localhost:8083/v3/api-docs/procedure-service`
- Pharmacy Service: `http://localhost:8083/v3/api-docs/pharmacy-service`
- Ops Service: `http://localhost:8083/v3/api-docs/ops-service`
- Core Ops Service (Python): `http://localhost:8083/v3/api-docs/core-ops-service`

## Core-Ops RabbitMQ Endpoints (Python)

Direct core-ops:
- Health: `GET http://localhost:8086/api/core-ops/rabbitmq/health`
- Bind queue: `POST http://localhost:8086/api/core-ops/rabbitmq/bind-queue`
- Publish message: `POST http://localhost:8086/api/core-ops/rabbitmq/publish`
- Consume one message: `POST http://localhost:8086/api/core-ops/rabbitmq/consume-once`

Via gateway:
- Health (public): `GET http://localhost:8083/api/core-ops/rabbitmq/health`
- Bind queue (secured): `POST http://localhost:8083/api/core-ops/rabbitmq/bind-queue`
- Publish message (secured): `POST http://localhost:8083/api/core-ops/rabbitmq/publish`
- Consume one message (secured): `POST http://localhost:8083/api/core-ops/rabbitmq/consume-once`