# communication-service

## Workflow change log

### Step 3 - Internal Staff Messaging

Date: 2026-05-01

Purpose:

- Provide a separate staff-only direct messenger inside the backoffice.
- Keep internal staff chat isolated from guardian communication and its `follow_up_messages` workflow.
- Support REST history/read state plus websocket delivery for new messages.

#### Files modified

- `FrontEnd/src/app/app.routes.ts`
- `FrontEnd/src/app/core/models/internal-staff-messaging.models.ts`
- `FrontEnd/src/app/core/services/internal-staff-messaging-api.service.ts`
- `FrontEnd/src/app/core/services/internal-staff-messaging-realtime.service.ts`
- `FrontEnd/src/app/layouts/backoffice-layout/backoffice-layout.ts`
- `FrontEnd/src/app/pages/backoffice/internal-staff-messaging/internal-staff-messaging.ts`
- `FrontEnd/src/app/pages/backoffice/internal-staff-messaging/internal-staff-messaging.html`
- `FrontEnd/src/app/pages/backoffice/internal-staff-messaging/internal-staff-messaging.scss`
- `FrontEnd/src/environments/environment.ts`
- `BackEnd/microservices/communication-service/pom.xml`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/config/SecurityConfig.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/security/CurrentUserService.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/client/UserServiceClientFeign.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/integration/UserDirectoryClient.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/integration/dto/StaffSearchRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/integration/dto/StaffSearchResponse.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/integration/dto/UserSummary.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/staffmessaging/**/*`
- `BackEnd/microservices/communication-service/src/main/resources/db/migration/V5__create_internal_staff_messaging.sql`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/controller/InternalStaffMessagingControllerWebMvcTest.java`
- `BackEnd/microservices/communication-service/src/test/java/tn/esprit/spring/communicationservice/service/impl/InternalStaffMessagingServiceImplTest.java`
- `BackEnd/microservices/communication-service/README.md`
- `docs/INTERNAL_STAFF_MESSAGING_CHANGES.md`

#### Backend changes

- Added a separate internal staff messaging module with its own tables, entities, repositories, service layer, REST controller, and websocket delivery path.
- Direct conversations are isolated from guardian communication and do not reuse `follow_up_messages`.
- Access is limited to `ADMIN`, `HR`, `DOCTOR`, `NURSE`, `RECEPTIONIST`, `PHARMACIST`, `LAB_AGENT`, and `SURGEON`.
- Conversation history is participant-only. Non-participants cannot read or send inside a conversation.
- Read state is tracked by participant with `last_read_at`, and unread counts are derived from message history.

#### Frontend changes

- Added a dedicated backoffice page and route for `Internal Staff Messaging`.
- Added a separate sidebar entry for staff roles only.
- Built a split-view messenger UI with staff search, conversation list, full thread history, unread badges, timestamps, and empty/loading/error states.
- Added a native websocket client with automatic reconnect and REST refresh fallback when live delivery is unavailable.
- Registered the page under the `BackofficeLayoutComponent` child route `/backoffice/internal-staff-messaging`.
- Route and sidebar visibility use the shared staff-role allowlist so `RECEPTIONIST` and the other allowed staff roles can open the page, while `GUARDIAN` stays blocked.

#### DB/migration changes

- Added Flyway migration `V5__create_internal_staff_messaging.sql`.
- New tables:
  - `staff_conversations`
  - `staff_conversation_participants`
  - `staff_messages`

#### Endpoint and realtime changes

- New REST endpoints:
  - `GET /api/communication/staff/users`
  - `POST /api/communication/staff/conversations/direct`
  - `GET /api/communication/staff/conversations`
  - `GET /api/communication/staff/conversations/{conversationId}/messages`
  - `POST /api/communication/staff/conversations/{conversationId}/messages`
  - `PUT /api/communication/staff/conversations/{conversationId}/read`
- New websocket endpoint:
  - `GET /ws/staff-messaging?token=<jwt>`
- Websocket delivery is server-push only. REST remains the source of truth for history and read state.

#### Tests performed

- `mvn -q -DskipTests compile` in `BackEnd/microservices/communication-service`
- `mvn -q "-Dtest=InternalStaffMessagingServiceImplTest,InternalStaffMessagingControllerWebMvcTest" test` in `BackEnd/microservices/communication-service`
- `npm run build` in `FrontEnd`

#### Known remaining issues

- Full `mvn -q test` is still not green in this environment because `CommunicationServiceApplicationTests` starts without a configured datasource.
- Live websocket delivery currently uses the direct communication-service websocket URL configured in `FrontEnd/src/environments/environment.ts`. No API-gateway websocket route was added in this step.

#### 2026-05-03 runtime stability fix

Root cause:

- The internal staff messaging 401/session failures were caused by unstable backend startup, not by the staff messenger domain itself.
- Several Spring services were starting before `config-server` was healthy, then fetching partial or conflicting configuration.
- Bootstrap-based services also had a duplicated config-client setup, with `bootstrap.yml` and `application.yml` both trying to drive config-server resolution. In Docker that led to mixed startup behavior against both `config-server:8888` and `localhost:8888`.
- When `user-service` came up half-configured or failed during datasource setup, `/api/auth/login` and `/api/auth/refresh` became unreliable. Once refresh failed, the frontend cleared the session and downstream staff messaging requests started returning `401`.

Fix applied:

- Added health checks and health-gated `depends_on` sequencing in `docker-compose.full.yml` for `keycloak`, `eureka`, `config-server`, `user-service`, `administration-service`, `communication-service`, and `api-gateway`.
- Kept `api-gateway` on explicit `SPRING_CONFIG_IMPORT=configserver:http://config-server:8888`.
- For bootstrap-based microservices, standardized `application.yml` to use a single neutral import:
  - `spring.config.import: "configserver:"`
- Kept the actual config-server address in Docker environment variables through:
  - `SPRING_CLOUD_CONFIG_URI=http://config-server:8888`
  - `SPRING_CLOUD_CONFIG_FAIL_FAST=true`
- This removes the `localhost:8888` conflict while still satisfying Spring Boot 3 config import requirements.

Runtime result after the fix:

- `user-service`, `administration-service`, `communication-service`, and `api-gateway` now start in a stable order and report `UP` health.
- `POST /api/auth/login` works again through the gateway.
- `POST /api/auth/refresh` works again through the gateway.
- Internal staff messaging endpoints under `/api/communication/staff` work without the previous session-expiry/401 chain.

### Step 2 - Guardian Communication Workflow Completion

Date: 2026-05-01

#### Files modified

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

#### Backend changes

- Quick-reply templates are now permanently scoped to the logged-in staff role.
- Existing templates are backfilled by Flyway so receptionist templates stay with receptionist-facing message types and nurse templates stay with nurse-facing message types.
- Template list, update, delete, and usage operations now reject cross-role access.

#### Frontend changes

- Backoffice message detail now shows an explicit loading state and a visible error state instead of rendering a blank body when message fetch fails.
- Nurse detail view keeps the escalation action visible for open conversations and shows the assigned doctor on the case summary.
- Doctor inbox highlights escalated conversations and surfaces an escalated-case count.
- Template management now shows the owning staff role for each template.

#### DB/migration changes

- Added Flyway migration `V4__scope_quick_reply_templates_by_staff_role.sql`.
- The migration adds `quick_reply_templates.staff_role`, backfills existing rows, marks the column `NOT NULL`, and creates supporting indexes.

#### Tests performed

- `mvn -q test` in `BackEnd/microservices/communication-service`
- `npm run build` in `FrontEnd`

#### Known remaining issues

- Browser/manual end-to-end role verification still depends on seeded guardian, receptionist, nurse, and doctor accounts.

### Step 1 - Guardian Communication Triage Stabilization

Date: 2026-05-01

#### Files modified

- `FrontEnd/src/app/core/services/communication-api.service.ts`
- `FrontEnd/src/app/pages/frontoffice/communication-thread/communication-thread.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.ts`
- `FrontEnd/src/app/pages/backoffice/communication-inbox/communication-inbox.html`
- `FrontEnd/src/app/pages/backoffice/communication-details/communication-details.ts`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/dto/request/EscalateRequest.java`
- `BackEnd/microservices/communication-service/src/main/java/tn/esprit/spring/communicationservice/service/impl/FollowUpMessageServiceImpl.java`
- `BackEnd/microservices/communication-service/README.md`
- `docs/COMMUNICATION_WORKFLOW_CHANGES.md`

#### Backend changes

- Guardian message routing now sends `ADMINISTRATIVE` and `APPOINTMENT` to `RECEPTIONIST`.
- `MEDICAL`, `LAB_RESULT`, and `OTHER` now route to `NURSE`.
- `QUESTION` and `COMPLAINT` route to `NURSE` only when priority is `HIGH`; otherwise they route to `RECEPTIONIST`.
- Closed messages can no longer be taken, unassigned, marked read, replied to, escalated, or closed again.
- Nurse escalation now requires a selected doctor and reason.
- Escalation assigns the selected doctor consistently through `queue`, `assignedToRole`, `assignedToUserKeycloakId`, and `assignedDoctorKeycloakId`.
- Escalation reason is stored in `message_audit_logs.details` for the `ESCALATED` audit action.

#### Frontend changes

- Guardian message detail now times out after 15 seconds and shows a visible error instead of staying on infinite loading.
- Doctor directory lookup now uses `GET /api/communication/backoffice/doctors`.
- Backoffice inbox default view now includes unassigned queue messages and messages assigned to the current staff user.
- Nurse escalation now sends the reason in the escalation request instead of adding it as a visible conversation reply.

#### DB/migration changes

- No DB migration changes were made in Step 1.
- Escalation reason uses the existing audit log details column.

#### Endpoint/routing changes

- `POST /api/communication/messages/{id}/escalate` now expects both `doctorKeycloakId` and `reason`.
- `GET /api/communication/backoffice/doctors` is the frontend doctor directory endpoint.

#### Tests performed

- `mvn -q test` in `BackEnd/microservices/communication-service`
- `npm install` in `FrontEnd` to restore missing local Angular build dependencies
- `npm run build` in `FrontEnd`

#### Test results

- Communication-service tests passed.
- Frontend Angular build passed.
- Frontend build reported existing bundle/CSS budget warnings in unrelated pharmacy and guardian/frontoffice styles.
- `npm install` reported existing audit warnings. No audit fix was run.
- Browser/manual role-flow verification was not run in this step.

#### Known remaining issues

- Quick-reply templates remain shared by message type across staff roles.
- Internal Staff Messaging was not implemented.
- No AI/content classification was added.
- Full end-to-end role testing still needs seeded guardian, receptionist, nurse, and doctor sessions.

## Required environment variables

- `COMM_DB_HOST`
- `COMM_DB_PORT`
- `COMM_DB_NAME`
- `COMM_DB_USER`
- `COMM_DB_PASSWORD`
- `KEYCLOAK_ISSUER_URI` (optional, default: `http://localhost:8080/realms/nephrospaidi`)
- `EUREKA_DEFAULT_ZONE` (optional)

## Endpoints summary

### Communication

- `POST /api/communication/messages`
- `GET /api/communication/messages/my`
- `GET /api/communication/messages/{id}`
- `POST /api/communication/messages/{id}/reply`
- `POST /api/communication/messages/{id}/close`
- `POST /api/communication/messages/{id}/take`
- `POST /api/communication/messages/{id}/mark-read`
- `POST /api/communication/messages/{id}/escalate`
- `GET /api/communication/backoffice/inbox`
- `GET /api/communication/messages/{id}/audit`
- `GET /api/communication/staff/users`
- `POST /api/communication/staff/conversations/direct`
- `GET /api/communication/staff/conversations`
- `GET /api/communication/staff/conversations/{conversationId}/messages`
- `POST /api/communication/staff/conversations/{conversationId}/messages`
- `PUT /api/communication/staff/conversations/{conversationId}/read`

### Internal staff messaging

Purpose:

- Staff-only direct messaging inside the backoffice.
- Separate persistence model from guardian communication.
- Websocket push for new-message delivery with REST fallback for inbox, history, and read state.

Allowed roles:

- `ADMIN`
- `HR`
- `DOCTOR`
- `NURSE`
- `RECEPTIONIST`
- `PHARMACIST`
- `LAB_AGENT`
- `SURGEON`

Blocked roles:

- `GUARDIAN`
- any non-staff user

REST endpoints under `/api/communication/staff`:

- `GET /users`
- `POST /conversations/direct`
- `GET /conversations`
- `GET /conversations/{conversationId}/messages`
- `POST /conversations/{conversationId}/messages`
- `PUT /conversations/{conversationId}/read`

Websocket:

- `/ws/staff-messaging?token=<jwt>`

Frontend route:

- `/backoffice/internal-staff-messaging`
- Rendered inside `BackofficeLayoutComponent`
- Sidebar item shown only for the allowed staff roles above

### Appointments

- `POST /api/appointments/requests`
- `GET /api/appointments/my`
- `GET /api/appointments/requests`
- `POST /api/appointments/requests/{id}/approve`
- `POST /api/appointments/requests/{id}/reject`
- `POST /api/appointments/requests/{id}/cancel`

## Roles access matrix

| Action | GUARDIAN | RECEPTIONIST | NURSE | DOCTOR |
|---|---|---|---|---|
| Create message | ✅ | ❌ | ❌ | ❌ |
| My messages | ✅ | ❌ | ❌ | ❌ |
| Inbox | ❌ | ✅ (RECEPTIONIST queue) | ✅ (NURSE queue) | ✅ (DOCTOR queue) |
| Take / mark-read / reply / close | Own messages only | Queue or assigned | Queue or assigned | Queue or assigned |
| Escalate | ❌ | ❌ | ✅ | ❌ |
| Create appointment request | ✅ | ❌ | ❌ | ❌ |
| View own appointment requests | ✅ | ❌ | ❌ | ❌ |
| List appointment requests | ❌ | ✅ | ❌ | ❌ |
| Approve / reject appointment request | ❌ | ✅ | ❌ | ❌ |
| Cancel appointment request | ✅ (own + status REQUESTED/APPROVED) | ❌ | ❌ | ❌ |
## Internal staff messaging access

| Action | ADMIN | HR | DOCTOR | NURSE | RECEPTIONIST | PHARMACIST | LAB_AGENT | SURGEON | GUARDIAN |
|---|---|---|---|---|---|---|---|---|---|
| List staff users | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âŒ |
| Create/open direct conversation | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âŒ |
| View own conversations | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âœ… | âŒ |
| Read/send inside participant conversation | Participant only | Participant only | Participant only | Participant only | Participant only | Participant only | Participant only | Participant only | âŒ |
