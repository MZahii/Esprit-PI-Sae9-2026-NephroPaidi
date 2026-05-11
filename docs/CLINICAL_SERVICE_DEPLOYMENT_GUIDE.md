# Clinical Service - Deployment & Integration Guide

## Overview

The **Clinical Service** is a Spring Boot 3.5.10 microservice providing REST APIs for pediatric nephrology clinical data management with AI-powered clinical alerting.

### Key Features
- RESTful endpoints for consultations, hospitalizations, discharge documents, and clinical alerts
- AI-powered clinical recommendations via async event processing
- HAS (Hospitalized and School-aged) discharge documentation compliance
- Neon PostgreSQL database with Flyway migrations
- Async event-driven architecture for non-blocking AI integration
- Resilience4j circuit breaker for fault tolerance
- OpenAPI/Swagger documentation at `/swagger-ui.html`

---

## Architecture

### Technology Stack
- **Framework:** Spring Boot 3.5.10 (Java 17)
- **Database:** PostgreSQL (Neon Cloud)
- **Message Bus:** RabbitMQ (optional, for future enhancements)
- **Service Discovery:** Eureka
- **Inter-Service Communication:** Feign HTTP clients + Circuit Breaker
- **AI Integration:** Async event listeners + Feign client
- **API Documentation:** SpringDoc OpenAPI 2.x

### Core Components

#### REST Controllers (5)
1. **ConsultationController** - CRUD for consultation records
2. **HospitalizationController** - CRUD for hospitalization episodes
3. **DischargeDocumentController** - CRUD + HAS compliance validation
4. **ClinicalAlertController** - Query and manage clinical alerts
5. **ClinicalOperationsController** - Specialized clinical operations (CKD computation, validation)

#### Services (3)
1. **ClinicalValidationService** - Business logic for Schwartz formula, minimal change disease detection
2. **ClinicalAlertsService** - BP classification, electrolyte checking
3. (Future) AI integration service

#### Event-Driven Components
- **ConsultationCreatedEvent** - Published when consultation saved
- **ConsultationCreatedEventListener** - Async processor calling AI service
- **AIClinicalServiceClient** - Feign client for AI predictions

#### Database
- 7 entities mapped to database tables
- 4 Flyway migrations (V21-V24) for schema management
- Indexes on frequently queried columns (patient_id, dates, severity)

---

## Deployment

### Local Development

#### 1. Prerequisites
```bash
# Java 17
java -version

# Maven 3.8+
mvn -version

# Docker & Docker Compose
docker --version
docker-compose --version
```

#### 2. Start Infrastructure
```bash
cd /path/to/Esprit-PI-Sae9-2026-NephroPaidi

# Full stack (Keycloak, RabbitMQ, all services)
docker-compose -f docker-compose.full.yml up -d

# Or minimal stack
docker-compose -f docker-compose.dev.yml up -d
```

#### 3. Build & Run Clinical Service
```bash
cd BackEnd/microservices/clinical-service

# Build
mvn clean package -DskipTests

# Run
java -jar target/clinical-service-0.0.1-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

#### 4. Access Services
- **Clinical API:** http://localhost:8084
- **API Documentation:** http://localhost:8084/swagger-ui.html
- **Eureka Dashboard:** http://localhost:8761
- **Config Server:** http://localhost:8888

---

### Docker Deployment

The clinical-service is already configured in `docker-compose.full.yml`:

```yaml
clinical-service:
  build:
    context: ./BackEnd/microservices/clinical-service
  container_name: nephro-clinical-service
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://neon-db:5432/clinical_service
    SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
    EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka:8761/eureka
    AI_CLINICAL_SERVICE_URL: http://ai-clinical-service:8091
  ports:
    - "8084:8084"
  depends_on:
    - eureka
    - ai-clinical-service
  networks:
    - nephro-net
```

**Start:**
```bash
docker-compose -f docker-compose.full.yml up -d clinical-service
```

---

### Neon PostgreSQL Configuration

#### Environment Variables
```bash
# Neon Cloud connection string
SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-endpoint>:5432/<database>?sslmode=require&channel_binding=require
SPRING_DATASOURCE_USERNAME=<neon_owner>
SPRING_DATASOURCE_PASSWORD=<neon_password>

# Or use Neon console:
# 1. Connect string: postgresql://user:password@ep-xxxx.c-2.eu-central-1.aws.neon.tech/dbname?sslmode=require
# 2. Spring will auto-enable SSL
```

#### Flyway Migrations
Migrations run automatically on application startup:
- **V21:** AI metadata columns (`parser_confidence`, `contentType`, etc.)
- **V22:** Hospitalization records table
- **V23:** Discharge documents + related tables (technical acts, medications, follow-up plans)
- **V24:** Clinical alerts table

**View migration status:**
```bash
curl http://localhost:8084/actuator/health
```

---

## AI Service Integration

### Architecture

The clinical-service integrates with `ai-clinical-service` (Python Flask) for clinical alert recommendations:

```
Consultation Saved
    ↓
ConsultationCreatedEvent published (async)
    ↓
ConsultationCreatedEventListener triggered
    ↓
Extract features (age, sex, creatinine, confidence)
    ↓
AIClinicalServiceClient.predict(features) → HTTP POST to /predict
    ↓
AI returns: {prediction, confidence, class_probabilities}
    ↓
Map prediction → AlertSeverity (URGENT/WARNING/ROUTINE)
    ↓
ClinicalAlert created in database
    ↓
Clinical staff reviews alert
```

### Configuration

**application.yml:**
```yaml
services:
  ai-clinical:
    base-url: ${AI_CLINICAL_SERVICE_URL:http://ai-clinical-service:8091}
```

**AI Service Endpoint Expected:**
```http
POST /predict
Content-Type: application/json

{
  "ageYears": 5,
  "sex": "M",
  "creatinine_mg_dl": 0.8,
  "creatinine_umol_l": 70.8,
  "parserConfidence": 0.92,
  "has_creatinine": true,
  "content_type": "application/pdf",
  "requires_manual_review": false
}
```

**Expected Response:**
```json
{
  "prediction": "urgent",
  "confidence": 0.78,
  "class_probabilities": {
    "other": 0.05,
    "review": 0.12,
    "urgent": 0.78,
    "warning": 0.05
  }
}
```

### Fallback Behavior

If AI service is unavailable:
- Fallback client returns conservative recommendation: `"review"`
- Clinical consultation still saved successfully (async processing)
- No cascade failure or API error

---

## API Endpoints

### Consultation Endpoints

**Create Consultation**
```http
POST /api/v1/consultations
Content-Type: application/json

{
  "patientId": "uuid",
  "consultationType": "INITIAL",
  "admissionMode": "AMBULATORY",
  "consultationDate": "2024-05-09T14:30:00",
  "chiefComplaint": "Nephrotic syndrome",
  "weight_kg": 25.5,
  "height_cm": 125,
  "serumCreatinine_mgdL": 0.8,
  "parserConfidence": 0.92,
  "contentType": "application/pdf"
}

Response: 201 CREATED
```

**Get Consultation**
```http
GET /api/v1/consultations/{id}
Response: 200 OK with consultation data
```

**Get Patient Consultations**
```http
GET /api/v1/consultations?patientId={uuid}
Response: 200 OK with list of consultations (ordered by date DESC)
```

**Update Consultation**
```http
PUT /api/v1/consultations/{id}
Response: 200 OK with updated data
```

**Delete Consultation**
```http
DELETE /api/v1/consultations/{id}
Response: 204 NO CONTENT
```

### Clinical Operations Endpoints

**Compute CKD Stage**
```http
POST /api/v1/clinical/consultations/{id}/compute-ckd?ageYears=5&isPremature=false
Response: 200 OK with updated consultation + computed eGFR
```

**Validate Schwartz Requirements**
```http
POST /api/v1/clinical/consultations/{id}/validate
Response: 200 OK "Consultation validated successfully"
```

**Scan Clinical Alerts**
```http
POST /api/v1/clinical/consultations/{id}/scan-alerts
Response: 200 OK "Alert scan completed"
```

**Check Minimal Change Disease**
```http
POST /api/v1/clinical/consultations/{id}/check-minimal-change?ageYears=5
Response: 200 OK "Minimal change disease likely - Pattern detected"
```

### Clinical Alert Endpoints

**Get Alert**
```http
GET /api/v1/alerts/{id}
Response: 200 OK with alert data
```

**Get Patient Alerts**
```http
GET /api/v1/alerts?patientId={uuid}
Response: 200 OK with list of unresolved alerts (ordered by date DESC)
```

**Get Urgent Alerts**
```http
GET /api/v1/alerts/urgent?patientId={uuid}
Response: 200 OK with URGENT and WARNING alerts only
```

**Acknowledge Alert**
```http
POST /api/v1/alerts/{id}/acknowledge
Response: 200 OK - sets acknowledgedAt timestamp
```

**Resolve Alert**
```http
POST /api/v1/alerts/{id}/resolve
Response: 200 OK - sets resolved=true and resolvedAt=now()
```

---

## Monitoring & Observability

### Actuator Endpoints
```
GET  /actuator/health           - Service health
GET  /actuator/info             - Application info
GET  /actuator/metrics          - Prometheus metrics
GET  /actuator/prometheus       - Prometheus format
```

### Logging
```yaml
logging:
  level:
    tn.esprit.spring.clinicalservice: DEBUG
    org.springframework.web: INFO
```

### Metrics
- Request count/latency
- Database connection pool
- Circuit breaker state
- Event listener performance

---

## Testing

### Integration Tests

Test structure created in `src/test/java/`:
- **BaseIntegrationTest** - Common setup for all tests
- **ConsultationControllerIT** - Test CRUD endpoints
- (Future) Additional controller tests

**Run tests:**
```bash
mvn test

# Or specific test
mvn test -Dtest=ConsultationControllerIT
```

### Manual Testing

**Using cURL:**
```bash
# Create consultation
curl -X POST http://localhost:8084/api/v1/consultations \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "consultationType": "INITIAL",
    "admissionMode": "AMBULATORY",
    "consultationDate": "2024-05-09T14:30:00",
    "chiefComplaint": "Nephrotic syndrome",
    "serumCreatinine_mgdL": 0.8
  }'

# Get consultations
curl http://localhost:8084/api/v1/consultations?patientId=550e8400-e29b-41d4-a716-446655440000

# Check alerts
curl http://localhost:8084/api/v1/alerts?patientId=550e8400-e29b-41d4-a716-446655440000
```

**Using Swagger UI:**
Navigate to http://localhost:8084/swagger-ui.html for interactive API testing.

---

## Troubleshooting

### Issue: Consultation Creation Hangs
**Cause:** AI service unavailable
**Solution:** Check AI service logs; fallback mechanism returns "review" recommendation

### Issue: Database Flyway Migration Failed
**Cause:** Schema already exists or constraint violation
**Solution:**
```bash
# Repair migrations
# In application.yml: flyway.repairOnMigrate: true (already set)

# Or manually check Neon:
# 1. Connect via Neon console
# 2. SELECT * FROM flyway_schema_history;
```

### Issue: Feign Client Connection Timeout
**Cause:** Service not reachable
**Solution:**
```yaml
feign:
  client:
    config:
      ai-clinical-service:
        connectTimeout: 5000
        readTimeout: 10000
```

---

## Environment Variables Reference

```bash
# Database (Neon PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=...

# Service Discovery
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka
EUREKA_INSTANCE_HOSTNAME=clinical-service

# AI Service
AI_CLINICAL_SERVICE_URL=http://ai-clinical-service:8091

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_TN_ESPRIT=DEBUG
```

---

## Version History

### v1.0.0 (Current)
- ✅ REST API layer (5 controllers, 19 files)
- ✅ Database schema with Flyway migrations
- ✅ AI integration with async event processing
- ✅ Clinical alert generation
- ✅ HAS discharge document compliance
- ⏭️ Manual integration testing
- ⏭️ Performance testing

---

## References

- [Spring Boot 3.5.10 Documentation](https://docs.spring.io/spring-boot/docs/3.5.10/reference/html/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.0)
- [Neon PostgreSQL Documentation](https://neon.tech/docs/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [SpringDoc OpenAPI](https://springdoc.org/)
