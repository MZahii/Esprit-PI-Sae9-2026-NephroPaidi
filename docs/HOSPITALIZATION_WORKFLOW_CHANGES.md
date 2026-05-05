# Hospitalization Workflow Changes

## Step 1 - Hospitalization Location And Nurse Task Traceability

Date: 2026-05-01

### Root causes addressed

- Hospitalization cases had no persisted room/bed fields, so receptionist assignment and location visibility were impossible.
- Nurse traceability stored only a current task state plus limited nurse identity fields, which made multi-nurse collaboration hard to audit clearly.
- The UI showed some execution history for doctors, but not enough structured actor/action detail, and nurse views did not surface location/history well.

### Files modified

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

### Backend changes

- Added hospitalization location assignment through `PUT /api/hospitalizations/{id}/location` for receptionist users.
- Added `roomNumber` and `bedNumber` to hospitalization case responses and summaries.
- Enriched nurse task execution history with:
  - `actionPerformed`
  - `nurseKeycloakId`
  - `nurseUsername`
  - `nurseDisplayName`
  - status/value/note/unit/timestamp
- Kept current task status fields for quick reads while preserving each nurse update as an append-only execution record.

### Frontend changes

- Hospitalization detail route now works for receptionist users and includes room/bed assignment UI.
- Doctor and nurse screens now show room/bed location.
- Nurse task list now displays execution history with actor/action/timestamp details.
- No manual signature field is used for hospitalization task execution.

### DB/migration changes

- Added Flyway migration `V3__add_hospitalization_location_and_task_traceability.sql`.
- This migration adds:
  - `hospitalization_case.room_number`
  - `hospitalization_case.bed_number`
  - `hospitalization_task.last_updated_by_nurse_display_name`
  - `hospitalization_task_execution.action_performed`
  - `hospitalization_task_execution.nurse_display_name`

### Tests performed

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=HospitalizationServiceImplTest,HospitalizationControllerWebMvcTest" test`
- `npm run build`

### Test notes

- Focused hospitalization backend tests passed.
- Frontend build passed.
- Full `mvn -q test` currently fails in `OpsServiceApplicationTests` against an external schema that has not yet applied migration `V3`; Hibernate reports missing `bed_number` during schema validation.
