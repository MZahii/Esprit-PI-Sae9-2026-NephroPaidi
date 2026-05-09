# Demo & Startup (migrated from PORTS_AND_STARTUP.md)

See `CONTRIBUTING.md` for quick start. This file contains the demo script and troubleshooting information used during presentations.

-- Demo script and full startup steps --

1. Start infrastructure

```bash
cd BackEnd
docker compose up -d
```

2. Start services locally (if desired)

Follow the Maven run instructions in each microservice folder.

3. Verify

- Eureka: http://localhost:8761
- API Gateway: http://localhost:8083
- Keycloak: http://localhost:8080
- User Service Swagger: http://localhost:8090/swagger-ui/index.html

Troubleshooting notes and ports are preserved here for reference.
