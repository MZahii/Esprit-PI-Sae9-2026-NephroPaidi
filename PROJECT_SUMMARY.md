# NephroPaidi - Full Doctor Workflow (TIER 2)

## Overview
Microservices platform for nephrology clinic. Built with Java Spring Boot (backend) + Angular (frontend).

## Architecture
- **Backend**: 8 microservices (clinical-service, user-service, pharmacy-service, etc.)
- **Frontend**: Angular standalone components, polling notifications (5s interval)
- **Database**: PostgreSQL Neon Cloud (nephros_user_service_db)
- **Gateway**: Spring Cloud Gateway (port 8083)

## Implemented Features ✅
1. **Doctor Dashboard** - Real-time metrics (appointments, requests, lab status, surgeries, hospitalizations)
2. **Lab Request Workflow** - Create requests → Upload results → Track status (PENDING/COMPLETED)
3. **Surgery Indication** - Doctor triggers surgery with urgency level, sent to receptionist
4. **Discharge Follow-Up** - Doctor creates follow-up instructions with dynamic items for patients
5. **Appointment Start Logic** - Start button available only TODAY within 15-min window, auto-cancel after 15min

## Key Entities
- `LabRequest`, `LabResult` → Lab workflow tracking
- `SurgeryIndication` → Surgery trigger & urgency
- `DischargeFollowUp`, `FollowUpItem` → Patient discharge instructions
- `Appointment` (existing) → Enhanced with start logic
- All use UUID (doctor_id) + BIGINT (patient_id) pattern

## Database Migrations
- **V8**: lab_requests table
- **V9**: surgery_indications table
- **V10**: discharge_follow_ups + follow_up_items tables
- **V11**: appointment timestamps (started_at, completed_at)

## API Endpoints
| Feature | Endpoint | Method |
|---------|----------|--------|
| Create Lab Request | `/api/clinical/lab-requests` | POST |
| Get My Requests | `/api/clinical/lab-requests/my` | GET |
| Upload Lab Result | `/api/clinical/lab-requests/{id}/results` | POST |
| Surgery Indication | `/api/clinical/surgery-indications` | POST |
| Dashboard | `/api/clinical/dashboard/my` | GET |
| Start Consultation | `/api/clinical/appointments/{id}/start` | POST |
| Discharge Follow-Up | `/api/clinical/discharge-follow-ups` | POST |

## Build Status ✅
- Backend: `mvn package -DskipTests` → JAR ready at `target/clinical-service-0.0.1-SNAPSHOT.jar`
- Frontend: `npm run build` → Distribution at `dist/duralux-admin/`
- All 38+ generated files compiling without errors

## Notification System
- Polling-based (existing system, NOT WebSocket)
- Frontend calls `/api/observability/notifications` every 5 seconds
- Service layer has TODO markers for notification integration

## Timeline
- Implementation: 3 hours (39 files generated)
- Compilation fixes: 1 hour (import paths, enum values, method signatures)
- Ready for: Database migration → Docker deployment → API testing
