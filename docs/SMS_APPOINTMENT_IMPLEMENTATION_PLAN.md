# NephrosPaidi SMS Appointment Plan

## Why this fits your current project

Your project already has the right base:
- Appointment request lifecycle in communication-service (`/api/appointments/requests`, approve, reject, cancel)
- Clinical appointment board in clinical-service
- Guardian identity resolution in communication-service
- API gateway routing for `/api/appointments/**` and `/api/communication/**`

Best low-risk strategy is to make SMS an extension of appointment request events in communication-service first, then optionally link deeper to clinical appointments.

## MVP scope (safe for demo)

Phase 1:
- Send SMS on appointment approval (confirmation)
- Send SMS on appointment rejection/cancelation (status update)
- Schedule one reminder SMS 24h before approved appointment
- Keep notification log with SENT/FAILED status and provider response

Do not start with inbound SMS reply parsing in MVP.

## Proposed architecture

Appointment module (already exists):
- Owns business decisions: REQUESTED -> APPROVED/REJECTED/CANCELLED

Notification module (new package in communication-service):
- Builds SMS text from templates
- Sends SMS through provider gateway client
- Stores delivery attempts and status
- Runs reminder scheduler

Provider adapter:
- SmsProvider interface
- TwilioSmsProvider implementation
- MockSmsProvider for local/dev

## Data model additions

### 1) sms_notification_log
- id (uuid)
- appointment_request_id (uuid, nullable for generic SMS)
- patient_id (bigint)
- guardian_user_id (bigint)
- phone_number (varchar)
- language (varchar, default en)
- message_type (CONFIRMATION, REMINDER_24H, CANCELLED, REJECTED, RESCHEDULED)
- message_body (varchar 1000)
- status (PENDING, SENT, FAILED)
- provider_name (TWILIO, MOCK)
- provider_message_id (varchar)
- provider_response (text)
- error_message (text)
- sent_at (timestamp)
- created_at (timestamp)

### 2) appointment_reminder_job
- id (uuid)
- appointment_request_id (uuid)
- due_at (timestamp)
- reminder_type (REMINDER_24H)
- status (PENDING, SENT, CANCELLED, FAILED)
- retry_count (int)
- last_attempt_at (timestamp)
- created_at (timestamp)
- updated_at (timestamp)

## Event flow inside communication-service

On APPROVE:
1. Persist appointment status APPROVED
2. Resolve guardian phone and preferred language
3. Create and send confirmation SMS
4. Create reminder job at scheduledDate - 24h

On REJECT:
1. Persist status REJECTED
2. Send rejection SMS
3. Cancel pending reminder jobs

On CANCEL:
1. Persist status CANCELLED
2. Send cancelation SMS
3. Cancel pending reminder jobs

Scheduler (every 5 min):
1. Load reminder jobs due now
2. Send SMS
3. Mark SENT or FAILED
4. Retry FAILED up to max attempts

## API additions (backoffice visibility)

Expose SMS history for traceability:
- GET `/api/appointments/requests/{id}/notifications`
- GET `/api/communication/sms/logs?status=&type=&from=&to=`

Optional resend endpoint for support:
- POST `/api/appointments/requests/{id}/notifications/resend`

## Template strategy

Use privacy-safe templates only. No diagnosis in SMS.

Examples:
- CONFIRMATION: "NephrosPaidi: Appointment approved for {childFirstName} on {date} at {time}."
- REMINDER_24H: "NephrosPaidi reminder: appointment tomorrow at {time}. Please bring requested documents."
- CANCELLED: "NephrosPaidi: Appointment on {date} has been cancelled. Contact the clinic for rescheduling."

## Required integration to user data

Current communication-service user integration returns only id/keycloakId/username.
For SMS, extend user lookup DTO to include:
- phone
- preferredLanguage

Then enforce:
- If phone missing -> do not fail appointment action
- Save FAILED log with reason "MISSING_PHONE"

## Frontend UX improvements

Backoffice appointment requests page:
- Show a small "Notification" badge per request: SENT / FAILED / PENDING
- Add "View SMS log" drawer
- If SMS failed, show clear reason (missing phone, provider down)

Guardian pages:
- Show "SMS notifications enabled" notice in appointment section
- Show non-blocking hint if phone is missing in profile

## Implementation order

1. Add DB migration for sms_notification_log and appointment_reminder_job
2. Add provider abstraction (Mock + Twilio)
3. Add notification service and template builder
4. Plug notification calls into approve/reject/cancel in AppointmentRequestServiceImpl
5. Add scheduler for reminder jobs
6. Add read APIs for notification logs
7. Add frontend status badge and SMS history modal

## Twilio config

Add to communication-service config:
- sms.provider=mock|twilio
- sms.twilio.accountSid
- sms.twilio.authToken
- sms.twilio.fromNumber
- sms.send.enabled=true

Use mock provider in dev by default.

## Demo script

1. Guardian creates appointment request
2. Receptionist approves request with scheduled date
3. System sends confirmation SMS and logs SENT
4. Backoffice opens request and sees SMS history
5. Simulate reminder scheduler execution for due reminder
6. Show reminder SMS log entry
7. Cancel appointment and show cancelation SMS log
