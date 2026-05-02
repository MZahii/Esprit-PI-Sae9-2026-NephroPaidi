# Swagger and Core-Ops Proof Checklist (For Viva)

Use this file during the demo when asked for proof.

## 1) "Springdoc/FastAPI OpenAPI activated"

### Java services (Springdoc dependency in pom.xml)
- Administration: BackEnd/microservices/administration-service/pom.xml#L89
- Communication: BackEnd/microservices/communication-service/pom.xml#L94
- Procedure: BackEnd/microservices/procedure-service/pom.xml#L105
- Ops: BackEnd/microservices/ops-service/pom.xml#L88

### Python service (FastAPI OpenAPI)
- OpenAPI path: BackEnd/microservices/core-ops-service/app/main.py#L16
- Swagger UI path: BackEnd/microservices/core-ops-service/app/main.py#L14

## 2) "Docs routes opened in security"

### Gateway security permitAll
- BackEnd/api-gateway/src/main/java/tn/esprit/spring/apigateway/security/GatewaySecurityConfig.java#L42

### Microservice security permitAll examples
- Communication: BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/config/SecurityConfig.java#L19
- Procedure: BackEnd/microservices/procedure-service/src/main/java/tn/esprit/spring/procedureservice/config/SecurityConfig.java#L28
- Ops: BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/security/SecurityConfig.java#L34

## 3) "Proxy routes added in API Gateway"

### Gateway OpenAPI/Swagger routes
- Administration OpenAPI route: BackEnd/api-gateway/src/main/resources/application.yml#L63
- Communication OpenAPI route: BackEnd/api-gateway/src/main/resources/application.yml#L113
- Procedure OpenAPI route: BackEnd/api-gateway/src/main/resources/application.yml#L133
- Ops OpenAPI route: BackEnd/api-gateway/src/main/resources/application.yml#L177
- Core-Ops fallback/openapi/swagger routes: BackEnd/api-gateway/src/main/resources/application.yml#L188

### Core-Ops target URI used by gateway
- BackEnd/api-gateway/src/main/resources/application.yml#L189
- docker-compose env: docker-compose.full.yml#L100

## 4) Live URLs to show in front of professor

### Direct (microservice)
- Core-Ops Swagger: http://localhost:8086/swagger-ui/index.html
- Administration OpenAPI JSON: http://localhost:8087/v3/api-docs

### Via Gateway (centralized)
- Core-Ops OpenAPI JSON: http://localhost:8083/v3/api-docs/core-ops-service
- Administration OpenAPI JSON: http://localhost:8083/v3/api-docs/administration-service

### Important URL note (very common mistake)
For gateway Swagger UI, use:
- http://localhost:8083/swagger/core-ops-service/index.html

Do NOT use:
- http://localhost:8083/swagger/core-ops-service/swagger-ui/index.html

Reason: the gateway rewrite already maps /swagger/core-ops-service/* to /swagger-ui/*.

## 5) Why Core-Ops is not visible in Eureka UI

Current architecture choice:
- Core-Ops runtime is Python FastAPI in Docker:
  - BackEnd/microservices/core-ops-service/Dockerfile#L1
  - BackEnd/microservices/core-ops-service/Dockerfile#L14
- API Gateway reaches Core-Ops by static service URI (not discovery lookup):
  - BackEnd/api-gateway/src/main/resources/application.yml#L189
  - docker-compose.full.yml#L100

So this is expected:
- Service works and is routable through gateway
- But it does not auto-register in Eureka like Spring Cloud Java services

One-line answer to professor:
"Core-Ops is intentionally integrated via gateway static URI. Eureka lists Spring discovery clients; Core-Ops is Python FastAPI and currently not a Eureka client."

## 6) Core-Ops RabbitMQ in Python (what changed)

Core-Ops is now the Python place for RabbitMQ operations.

Code proof:
- RabbitMQ client dependency: BackEnd/microservices/core-ops-service/requirements.txt
- RabbitMQ endpoints implementation: BackEnd/microservices/core-ops-service/app/main.py
- RabbitMQ runtime configuration: docker-compose.full.yml#L267

Available endpoints:
- GET /api/core-ops/rabbitmq/health
- POST /api/core-ops/rabbitmq/bind-queue
- POST /api/core-ops/rabbitmq/publish
- POST /api/core-ops/rabbitmq/consume-once

Gateway access:
- Health (public): GET http://localhost:8083/api/core-ops/rabbitmq/health
- Others are secured (through gateway auth), but directly testable on core-ops port 8086.

## 7) 60-second demo script

1. Open Eureka UI and show Java services registered (localhost:8761).
2. Explain Core-Ops integration mode (gateway static URI, not discovery).
3. Open direct Core-Ops Swagger:
   - http://localhost:8086/swagger-ui/index.html
4. Open centralized OpenAPI via gateway:
   - http://localhost:8083/v3/api-docs/core-ops-service
5. Open another service via gateway:
   - http://localhost:8083/v3/api-docs/administration-service
6. Conclusion:
   - "Direct docs + centralized gateway docs are both operational."

## 8) 45-second RabbitMQ demo script

1. Open health endpoint:
   - http://localhost:8086/api/core-ops/rabbitmq/health
2. In Swagger core-ops (direct), call bind-queue.
3. Call publish with a JSON payload.
4. Call consume-once and show the same payload returned.
5. Optional gateway proof:
   - http://localhost:8083/api/core-ops/rabbitmq/health

## 9) Existing reference guide in repo
- docs/SWAGGER_ACCESS_GUIDE.md

## 10) What to show for "frontend" of Core-Ops

Core-Ops does not currently have a dedicated Angular page in this repository.

Operational frontends to show instead:
- Core-Ops Swagger UI (direct): http://localhost:8086/swagger-ui/index.html
- Core-Ops Swagger/OpenAPI via gateway: http://localhost:8083/swagger/core-ops-service/index.html
- RabbitMQ Management UI: http://localhost:15672 (admin / admin123 by default)

How to explain this to jury:
- "Business frontend screens are in Angular; Core-Ops is an operational microservice."
- "Its functional UI is Swagger for API operations and RabbitMQ Management for broker visibility."
