# Communication Workflow Changes

## Step 2 - Guardian Communication Workflow Completion

Date: 2026-05-01

### Files modified

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-templates/communication-templates.html`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/domain/entity/QuickReplyTemplate.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/response/QuickReplyTemplateResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/repository/QuickReplyTemplateRepository.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/QuickReplyTemplateServiceImpl.java`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration/V4__scope_quick_reply_templates_by_staff_role.sql`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/QuickReplyTemplateServiceImplTest.java`
- `BackEnd/microservices/communication-service/README.md`
- `docs/COMMUNICATION_WORKFLOW_CHANGES.md`

### Root causes addressed

- The staff detail page rendered message content only when `message` existed, but had no fallback body when the fetch failed, so nurses could land on a header-only blank page.
- Doctor escalations were technically stored in the doctor queue, but the inbox did not visually prioritize or label escalated items, so they were easy to miss.
- Quick-reply templates were stored and listed only by `messageType`, so receptionist templates leaked into nurse and doctor workflows.

### Backend changes

- Quick-reply templates are now stored with `staffRole`.
- Template list/create/update/delete/use operations are restricted to the current staff role.
- Existing rows are migrated and backfilled with role ownership using message-type routing heuristics.

### Frontend changes

- Backoffice communication detail now shows explicit loading and error states instead of a blank body.
- Nurse detail view keeps doctor escalation available on open cases and shows the current assigned doctor.
- Doctor inbox now raises escalated cases visually and with a dedicated count.
- Template management now displays the owning role for each template.

### DB/migration changes

- Added Flyway migration `V4__scope_quick_reply_templates_by_staff_role.sql`.
- This migration adds `staff_role`, backfills existing data, sets `NOT NULL`, and adds indexes.

### Tests performed

- `mvn -q test` in `BackEnd/microservices/communication-service`
- `npm run build` in `FrontEnd`

## Step 1 - Guardian Communication Triage Stabilization

Date: 2026-05-01

### Files modified

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-thread/communication-thread.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/request/EscalateRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/README.md`
- `docs/COMMUNICATION_WORKFLOW_CHANGES.md`

### Backend changes

- Updated guardian message routing:
  - `ADMINISTRATIVE` and `APPOINTMENT` route to `RECEPTIONIST`.
  - `MEDICAL`, `LAB_RESULT`, and `OTHER` route to `NURSE`.
  - `QUESTION` and `COMPLAINT` route to `NURSE` only when `HIGH` priority, otherwise to `RECEPTIONIST`.
- Tightened closed-message transitions so closed conversations cannot be taken, unassigned, marked read, replied to, escalated, or closed again.
- Nurse escalation now requires a selected doctor and an escalation reason.
- Escalation now consistently sets:
  - `queue = DOCTOR`
  - `assignedToRole = DOCTOR`
  - `assignedToUserKeycloakId = selected doctor id`
  - `assignedDoctorKeycloakId = selected doctor id`
- Escalation reason is saved in the existing audit trail details for the `ESCALATED` action.

### Frontend changes

- Guardian message detail now times out after 15 seconds and shows a visible error instead of remaining in infinite loading.
- Guardian message detail state updates are run inside Angular view updates to keep loading/error/detail state visible.
- Doctor directory lookup now uses `/api/communication/backoffice/doctors`.
- Backoffice communication inbox default tab now shows unassigned messages plus messages assigned to the current staff user.
- Nurse escalation sends the selected doctor and reason directly to the escalation endpoint instead of storing the reason as a guardian-visible reply.

### DB/migration changes

- No DB migration changes were made in Step 1.
- Escalation reason is stored in existing `message_audit_logs.details`.

### Endpoint/routing changes

- Existing endpoint used for doctor directory: `GET /api/communication/backoffice/doctors`.
- Existing endpoint enhanced for escalation payload: `POST /api/communication/messages/{id}/escalate` now expects `doctorKeycloakId` and `reason`.
- Existing guardian message creation endpoint keeps the same path: `POST /api/communication/messages`.

### Tests performed

- `mvn -q test` in `BackEnd/microservices/communication-service`
- `npm install` in `FrontEnd` to restore missing local Angular build dependencies
- `npm run build` in `FrontEnd`

### Test results

- Communication-service tests passed.
- Frontend Angular build passed.
- Frontend build reported existing bundle/CSS budget warnings in unrelated pharmacy and guardian/frontoffice styles.
- `npm install` reported existing audit warnings. No audit fix was run.
- Browser/manual role-flow verification was not run in this step.

### Known remaining issues

- Quick-reply templates are still shared by message type across receptionist, nurse, and doctor. This is intentionally left for a later step.
- The workflow still uses the existing simple queue model; no Internal Staff Messaging was added.
- No AI/content classification was added for medical content in `QUESTION` or `COMPLAINT`.
- Full end-to-end role testing still needs seeded guardian, receptionist, nurse, and doctor sessions.
