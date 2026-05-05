# Internal Staff Messaging Changes

Date: 2026-05-01

## Scope

- Worked only in `communication-service` and the related Angular backoffice messaging UI.
- Did not modify guardian communication workflow logic.
- Did not modify hospitalization / ops-service.

## Root gaps found

- The existing communication module only covered guardian follow-up flows. There was no separate staff-only messaging domain.
- No dedicated tables existed for staff conversations, participants, unread state, or staff chat history.
- No websocket delivery path existed in `communication-service` or the Angular frontend for internal chat.
- Sidebar and routes had no staff-only messenger entry outside guardian communication.

## What was implemented

### Backend

- Added a new internal staff messaging module with:
  - `staff_conversations`
  - `staff_conversation_participants`
  - `staff_messages`
- Added staff-only REST endpoints under `/api/communication/staff`.
- Added participant access checks for history, send, and read operations.
- Added user-directory integration for staff search through `user-service`.
- Added a dedicated websocket endpoint at `/ws/staff-messaging`.

### Frontend

- Added a new backoffice page for `Internal Staff Messaging`.
- Added a staff-only route and sidebar item.
- Added live updates via native websocket.
- Kept REST as the fallback for history, send, and unread/read state.

## Security rules

- Allowed roles:
  - `ADMIN`
  - `HR`
  - `DOCTOR`
  - `NURSE`
  - `RECEPTIONIST`
  - `PHARMACIST`
  - `LAB_AGENT`
  - `SURGEON`
- Forbidden:
  - `GUARDIAN`
  - any non-staff user
- Conversation read/send access is participant-only.

## Websocket notes

- Frontend connects with `?token=<jwt>` because the browser native `WebSocket` API cannot attach bearer headers directly.
- The backend handshake validates the JWT and rejects non-staff roles.
- Delivery is server-push only; writes and read marking still go through REST.

## Migration

- Added `V5__create_internal_staff_messaging.sql`

## Validation summary

- Passed:
  - `mvn -q -DskipTests compile`
  - `mvn -q "-Dtest=InternalStaffMessagingServiceImplTest,InternalStaffMessagingControllerWebMvcTest" test`
  - `npm run build`
- Not fully green:
  - `mvn -q test` still fails because `CommunicationServiceApplicationTests` starts without a configured datasource in this environment.

## Remaining issue

- Live websocket delivery currently uses the direct communication-service URL from the Angular environment file. A gateway websocket route was not added in this step.

## 2026-05-03 frontend routing/sidebar fix

- Kept the page on the existing backoffice route `/backoffice/internal-staff-messaging`.
- Kept the page rendered inside `BackofficeLayoutComponent`; no public-layout route was added.
- Switched route protection and sidebar visibility to the shared staff-role allowlist already used by the backoffice shell.
- Corrected frontend role resolution so stored business roles remain usable when the JWT contains unrelated Keycloak roles but not a clean application role string.
- Result: `RECEPTIONIST` can open the internal staff messenger route and see the sidebar entry, while `GUARDIAN` remains blocked.

## 2026-05-03 receptionist visibility correction

- Verified that the active backoffice template renders the `navigationItems` array directly and does not apply any extra item-level filtering.
- Fixed the messenger sidebar visibility check to use the same resolved `role` already used by the rest of the receptionist navigation.
- Fixed `AuthStorageService.getRoles()` to keep the resolved stored business role alongside token-derived application roles so route guards and sidebar checks agree for staff users.
- Removed the temporary frontend console diagnostics after the root cause was confirmed.

## 2026-05-03 docker/runtime diagnostic

- Verified the current source tree contains the internal staff messaging route and sidebar entry.
- Verified the current local Angular build output contains `/backoffice/internal-staff-messaging`.
- Verified the live bundle served on `http://localhost:4200` is a different hashed artifact than the current local build and does not contain the internal staff messaging route.
- Added temporary console diagnostics in auth role resolution, the role guard, and the backoffice layout sidebar builder to trace:
  - resolved role
  - resolved roles array
  - role guard allow/deny decision
  - internal staff messaging sidebar visibility
- This diagnostic step does not change business logic. It is meant to confirm whether the running frontend picks up the expected source after a forced rebuild.

## 2026-05-03 runtime stability fix

- The later `401` and "session expired" errors on Internal Staff Messaging were not caused by the staff messaging module itself.
- Root cause was backend startup/config instability in Docker:
  - Spring services were allowed to start before `config-server` was healthy.
  - Several bootstrap-based services also had duplicated config-client behavior between `bootstrap.yml` and `application.yml`.
  - In practice that produced mixed config-server resolution against both `config-server:8888` and `localhost:8888`, which led to intermittent half-configured startups.
- The first visible symptom in the browser was usually `/api/auth/refresh` failing. Once refresh failed, the frontend cleared the session and `/api/communication/staff/conversations` started returning `401`.

What was fixed:

- Added health checks and health-gated startup ordering in `docker-compose.full.yml` for the core auth/config chain:
  - `keycloak`
  - `eureka`
  - `config-server`
  - `user-service`
  - `administration-service`
  - `communication-service`
  - `api-gateway`
- Standardized bootstrap-based microservices so `application.yml` uses:
  - `spring.config.import: "configserver:"`
- Kept the real Docker config-server location in environment variables via:
  - `SPRING_CLOUD_CONFIG_URI=http://config-server:8888`
  - `SPRING_CLOUD_CONFIG_FAIL_FAST=true`

Validation after the fix:

- `user-service`, `administration-service`, `communication-service`, and `api-gateway` report healthy actuator status.
- Gateway login works again.
- Gateway refresh works again.
- Internal staff messaging staff search, direct conversation creation, send, inbox, and history all work through `/api/communication/staff`.
- The doctor participant record and unread state were verified directly in the communication-service database.
