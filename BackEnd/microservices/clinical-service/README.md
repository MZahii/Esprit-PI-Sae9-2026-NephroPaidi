# Clinical Service README

## Overview

The **Clinical Service** is a Spring Boot microservice providing comprehensive REST APIs for pediatric nephrology clinical data management, consultation tracking, hospitalization records, discharge documentation (HAS-compliant), and AI-powered clinical alerting.

## Quick Start

### Local Development
```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# API Documentation
open http://localhost:8084/swagger-ui.html
```

### Docker
```bash
docker-compose -f docker-compose.full.yml up -d clinical-service
```

## Features

✅ **RESTful API for Clinical Data**
- Consultation records (SOAP notes, vitals, nephrology findings)
- Hospitalization episodes (neonatal/pediatric pathology tracking)
- Discharge documents (HAS §1-5 mandatory sections)
- Clinical alerts (AI recommendations + manual alerts)

✅ **AI-Powered Clinical Recommendations**
- Async event-driven architecture
- Feign HTTP client to AI service
- Automatic alert generation
- Graceful fallback when AI unavailable

✅ **Data Validation & Business Logic**
- Schwartz formula for CKD computation
- Minimal change disease detection
- BP classification and electrolyte checking
- HAS compliance validation

✅ **Database Management**
- Neon PostgreSQL with Flyway migrations
- Optimized indexes for performance
- Cascade delete with foreign key constraints
- Audit timestamps (created_at, updated_at)

✅ **Observability**
- Spring Boot Actuator health/metrics
- OpenAPI/Swagger documentation
- Structured logging (SLF4J)
- Circuit breaker metrics (Resilience4j)

## Architecture

```
┌─────────────────────────────────────────────┐
│         REST API Layer (5 Controllers)      │
├─────────────────────────────────────────────┤
│ - ConsultationController                    │
│ - HospitalizationController                 │
│ - DischargeDocumentController               │
│ - ClinicalAlertController                   │
│ - ClinicalOperationsController              │
├─────────────────────────────────────────────┤
│    Service Layer (Business Logic)           │
├─────────────────────────────────────────────┤
│ - ClinicalValidationService                 │
│ - ClinicalAlertsService                     │
├─────────────────────────────────────────────┤
│      Data Layer (Repositories)              │
├─────────────────────────────────────────────┤
│      Event-Driven Architecture              │
├─────────────────────────────────────────────┤
│ ConsultationCreatedEvent                    │
│        ↓                                     │
│ ConsultationCreatedEventListener (async)    │
│        ↓                                     │
│ AIClinicalServiceClient (Feign)             │
│        ↓                                     │
│ AI Service (/predict endpoint)              │
│        ↓                                     │
│ ClinicalAlert created in DB                 │
├─────────────────────────────────────────────┤
│     PostgreSQL (Neon Cloud)                 │
│     - 7 entities                            │
│     - 4 Flyway migrations                   │
└─────────────────────────────────────────────┘
```

## Key Endpoints

### Consultations
```
POST   /api/v1/consultations                 - Create
GET    /api/v1/consultations/{id}            - Get by ID
GET    /api/v1/consultations?patientId=X    - Get all for patient
PUT    /api/v1/consultations/{id}            - Update
DELETE /api/v1/consultations/{id}            - Delete
```

### Clinical Operations
```
POST   /api/v1/clinical/consultations/{id}/compute-ckd        - Compute CKD stage
POST   /api/v1/clinical/consultations/{id}/validate           - Validate Schwartz
POST   /api/v1/clinical/consultations/{id}/scan-alerts        - Scan BP/electrolytes
POST   /api/v1/clinical/consultations/{id}/check-minimal-change - MCD detection
```

### Alerts
```
GET    /api/v1/alerts/{id}                   - Get alert
GET    /api/v1/alerts?patientId=X            - Get patient alerts
GET    /api/v1/alerts/urgent?patientId=X    - Get urgent alerts only
POST   /api/v1/alerts/{id}/acknowledge      - Acknowledge alert
POST   /api/v1/alerts/{id}/resolve          - Resolve alert
```

### Hospitalizations & Discharges
```
POST   /api/v1/hospitalizations              - Create
GET    /api/v1/hospitalizations/{id}         - Get by ID
POST   /api/v1/discharges                    - Create discharge doc
POST   /api/v1/discharges/{id}/finalize      - Finalize + HAS validation
```

## Configuration

### Database (Neon PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://neon-endpoint:5432/db?sslmode=require
    username: ${NEON_USER}
    password: ${NEON_PASSWORD}
  flyway:
    outOfOrder: true
    validateOnMigrate: false
    repairOnMigrate: true
```

### AI Service Integration
```yaml
services:
  ai-clinical:
    base-url: ${AI_CLINICAL_SERVICE_URL:http://ai-clinical-service:8091}
```

### Async Configuration
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 2
        max-size: 5
        queue-capacity: 100
```

## Development

### Project Structure
```
src/
├── main/java/
│   └── tn/esprit/spring/clinicalservice/
│       ├── controller/          - REST endpoints (5 controllers)
│       ├── service/             - Business logic (2 services)
│       ├── entity/              - JPA entities (7)
│       ├── repository/          - Data access (7 repositories)
│       ├── dto/                 - Data transfer objects (8 DTOs)
│       ├── mapper/              - MapStruct mappers (6)
│       ├── event/               - Spring events (1)
│       ├── listener/            - Event listeners (1)
│       ├── client/              - Feign clients (AI service)
│       ├── enums/               - Enumerations (20+)
│       └── config/              - Spring configuration
├── resources/
│   ├── db/migration/            - Flyway migrations (V21-V24)
│   ├── application.yml          - Configuration
│   └── bootstrap.yml            - Config server
└── test/
    └── controller/              - Integration tests
```

### Building
```bash
# Full build with tests
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run specific phase
mvn compile
mvn test
```

### Dependencies
- Spring Boot 3.5.10
- Spring Data JPA
- Spring Cloud Feign
- MapStruct 1.6.3
- Lombok 1.18.42
- Flyway 9.22.x
- PostgreSQL JDBC 42.x
- SpringDoc OpenAPI 2.x

## Testing

### Integration Tests (Planned)
```bash
mvn test
```

Test structure created for:
- ConsultationControllerIT (CRUD operations)
- (More tests in development)

### Manual Testing
```bash
# Test API
curl -X POST http://localhost:8084/api/v1/consultations \
  -H "Content-Type: application/json" \
  -d '{"patientId":"uuid","consultationType":"INITIAL",...}'

# Check Swagger UI
open http://localhost:8084/swagger-ui.html
```

## Monitoring

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Metrics
```bash
curl http://localhost:8084/actuator/metrics
curl http://localhost:8084/actuator/prometheus
```

### Logs
```bash
tail -f logs/clinical-service.log
```

## Deployment

See [CLINICAL_SERVICE_DEPLOYMENT_GUIDE.md](../docs/CLINICAL_SERVICE_DEPLOYMENT_GUIDE.md) for:
- Docker deployment
- Neon PostgreSQL setup
- Environment configuration
- Troubleshooting guide

## Contributing

1. Clone the repository
2. Create feature branch (`git checkout -b feature/xxx`)
3. Make changes + test locally
4. Commit with descriptive message (`git commit -m "..."`)
5. Push to branch (`git push origin feature/xxx`)
6. Create Pull Request

## License

Apache License 2.0

## Support

For issues, questions, or contributions, contact the NephroPaidi development team.
