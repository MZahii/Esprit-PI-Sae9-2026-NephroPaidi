# ops-service

## Workflow change log

### Step 1 - Hospitalization Location And Nurse Task Traceability

Date: 2026-05-01

#### Files modified

- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/security/CurrentUserService.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/HospitalizationController.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/dto/AssignHospitalizationLocationRequest.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/dto/HospitalizationCaseResponse.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/dto/HospitalizationSummaryResponse.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/dto/HospitalizationTaskExecutionResponse.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/api/dto/HospitalizationTaskResponse.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/application/HospitalizationService.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/application/HospitalizationServiceImpl.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/domain/HospitalizationCase.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/domain/HospitalizationTask.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/domain/HospitalizationTaskExecution.java`
- `BackEnd/microservices/ops-service/src/main/java/tn/esprit/spring/opsservice/hospitalization/domain/HospitalizationTaskExecutionAction.java`
- `BackEnd/microservices/ops-service/src/main/resources/db/migration/V3__add_hospitalization_location_and_task_traceability.sql`
- `BackEnd/microservices/ops-service/src/test/java/tn/esprit/spring/opsservice/hospitalization/api/HospitalizationControllerWebMvcTest.java`
- `BackEnd/microservices/ops-service/src/test/java/tn/esprit/spring/opsservice/hospitalization/application/HospitalizationServiceImplTest.java`
- `FrontEnd/src/app/app.routes.ts`
- `FrontEnd/src/app/core/models/ops.models.ts`
- `FrontEnd/src/app/core/services/ops-api.service.ts`
- `FrontEnd/src/app/features/ops/hospitalizations/hospitalization-review.page.ts`
- `FrontEnd/src/app/features/ops/hospitalizations/hospitalization-review.page.html`
- `FrontEnd/src/app/features/ops/hospitalizations/nurse-hospitalizations.page.html`
- `BackEnd/microservices/ops-service/README.md`
- `docs/HOSPITALIZATION_WORKFLOW_CHANGES.md`

#### Backend changes

- Hospitalization cases now store `roomNumber` and `bedNumber`.
- Added receptionist endpoint `PUT /api/hospitalizations/{id}/location`.
- Nurse task executions now store:
  - `nurseKeycloakId`
  - `nurseUsername`
  - `nurseDisplayName`
  - `actionPerformed`
  - status/value/note/unit
  - timestamp
- Task history is append-only. Current task state is still updated for quick status reads, but each nurse action is preserved as its own execution record.

#### Frontend changes

- Receptionist can open hospitalization details and assign room/bed.
- Doctor and nurse hospitalization views show patient location.
- Nurse task list now shows execution history with actor identity, action, and timestamp.
- No manual signature input is used in the hospitalization task workflow.

#### DB/migration changes

- Added Flyway migration `V3__add_hospitalization_location_and_task_traceability.sql`.
- The migration adds case location columns plus display-name/action columns for nurse task traceability.

#### Tests performed

- `mvn -q -DskipTests compile` in `BackEnd/microservices/ops-service`
- `mvn -q "-Dtest=HospitalizationServiceImplTest,HospitalizationControllerWebMvcTest" test` in `BackEnd/microservices/ops-service`
- `npm run build` in `FrontEnd`

#### Known remaining issues

- Full `mvn -q test` currently fails in `OpsServiceApplicationTests` when the external test database schema has not yet applied migration `V3`, because Hibernate validates against a schema missing `bed_number`.
- There is still no receptionist-specific hospitalization list page; receptionist assignment currently happens from the hospitalization detail route.
