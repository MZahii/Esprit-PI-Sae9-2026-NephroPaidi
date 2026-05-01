# NephroPaidi Platform - Detailed Implementation Plan
**Version 1.0 | April 28, 2026**

---

## TIER 1: CRITICAL FOUNDATION (Weeks 1-6)

### 1.1 Login Page Enhancements
**Components:**
- Remember Me checkbox (persist login for 30 days via secure cookie)
- Forgot Password link → email reset form → token validation → new password entry
- Two-Factor Authentication (email/SMS OTP, 6-digit code, 5-minute expiry)
- Error messaging and password strength indicators

**Database Changes:**
- Add `remember_me_token` to users table
- Add `password_reset_tokens` table (token, user_id, expiry, used_flag)
- Add `2fa_enabled` flag and `2fa_method` (email/sms) to users

**API Endpoints:**
- `POST /api/auth/login` → Existing, add 2FA check
- `POST /api/auth/forgot-password` → Send reset email
- `POST /api/auth/reset-password` → Validate token and update password
- `POST /api/auth/verify-2fa` → Validate OTP code

---

### 1.2 Role-Based Access Control Refinement
**Role Hierarchy:**
```
ADMIN
├── Full system oversight
├── Supervise: HR + All Staff
├── No direct contract actions (view-only)
└── Access: All dashboards, all statistics

HR
├── Operational supervision of staff (except Admin)
├── Contract management: Add, update, delete
├── View staff performance statistics
└── Cannot supervise or manage Admin

RECEPTIONIST
├── Appointment management
├── Room/bed assignment
├── View minimal lab request details
└── Cannot access: Clinical data, prescriptions, surgery planning

DOCTOR
├── Patient consultations
├── Lab request sending
├── Prescription sending
├── Surgery indication
├── Appointment requests (NOT direct creation)
└── Cannot directly schedule appointments

NURSE
├── To-do list execution
├── Dialysis session documentation
├── Signature capability
└── Limited to assigned patients

LAB_AGENT
├── Test report uploads
├── Dashboard view
└── Limited to assigned patients

PHARMACIST
├── Stock management (3 types)
├── Prescription receiving/fulfilling
└── Stock control and supplier management

GUARDIAN
├── View child's medical records (limited)
├── Upload documents (lab, prescriptions, dialysis reports)
├── View follow-up lists
├── Print documents
└── Receive notifications and reminders
```

**Implementation:**
- Update `roles` table with permission matrix
- Create `role_permissions` junction table
- Middleware to check role before endpoint access
- UI elements show/hide based on role

---

### 1.3 Profile Settings & Security
**User Profile Features:**
- Edit personal information (name, email, phone)
- Change password (require current password)
- Session management (view active sessions, logout from other devices)
- Language preference (English, French, Arabic)
- Theme preference (light/dark mode)
- Notification preferences (email, SMS, in-app)

**Database:**
- Add `language_preference`, `theme_preference` to users
- Add `notification_preferences` table
- Add `user_sessions` table (track active sessions)

---

### 1.4 Left Sidebar Navigation Redesign
**Structure by Role:**

**ADMIN Navigation:**
- Dashboard
- User Management
- HR Management
  - Staff Contracts
  - Staff Performance
- Statistics & Analytics
- System Configuration
- Audit Logs
- Staff Messaging

**DOCTOR Navigation:**
- My Dashboard
- Today's Appointments
- All Appointments
- Patients
- Lab Requests
- Prescriptions
- Appointment Requests (sent to receptionist)
- Surgery Indications
- Messages

**RECEPTIONIST Navigation:**
- My Dashboard
- Appointments
  - Today's Appointments
  - All Appointments
  - Appointment Requests (from doctors)
- Patients
- Surgery Planning
- Room/Bed Assignment
- Lab Requests (view minimal details)
- Messages

**NURSE Navigation:**
- My Dashboard
- Assigned Patients
- To-Do Lists
- Dialysis Sessions
- Messages

**Implementation:**
- Sidebar component reads user role
- Dynamically render menu items based on permissions
- Collapsible sections for organization
- Icons + labels for clarity

---

### 1.5 Notification System (Application-Wide)
**Notification Types:**

**For Doctors:**
- Appointment reminders (1 hour before, 15 min before)
- Lab request completed
- Prescription fulfilled
- New patient message

**For Receptionist:**
- New appointment request from doctor
- New surgery indication from doctor
- Appointment modification request from guardian

**For Nurses:**
- New to-do list assigned
- Dialysis session reminder

**For HR:**
- Contract expiring soon
- Staff performance alert

**For Guardians:**
- Follow-up medication reminder
- Follow-up alert (urgent if non-compliant)
- Lab results ready

**Implementation:**
- `notifications` table (user_id, type, message, status, created_at)
- Real-time updates via WebSocket or polling
- Notification bell icon in header (unread count)
- Notification panel (dropdown or modal)
- Mark as read functionality

---

### 1.6 Home Page Dynamic Content Management
**Admin Interface:**
- WYSIWYG editor for:
  - Hero section (title, subtitle, image)
  - Feature sections (card-based, 3-5 cards)
  - Testimonials section
  - Contact information
  - Announcements/News section
- Image upload management
- Preview before publish
- Version history

**Database:**
- `home_page_sections` table (section_type, content, ordering, active_flag)
- `home_page_images` table (image_url, section_id)

---

### 1.7 Audit Logs Reorganization
**Features:**
- Searchable table with filters (user, action, date range, resource type)
- Sortable columns (date, user, action, resource)
- Pagination (25/50/100 items per page)
- Export to CSV/PDF
- Action details modal (what changed, old value, new value)
- Color-coded severity (critical, warning, info)

**Data to Log:**
- User login/logout
- Data modifications (create, update, delete)
- Password changes
- Permission changes
- Report generation
- File uploads

---

## TIER 2: CORE DOCTOR WORKFLOWS (Weeks 7-14)

### 2.1 Doctor Dashboard
**Components:**
- **Today's Appointments Card**
  - List of appointments scheduled for today
  - Time, patient name, type (consultation/follow-up)
  - Status badge (scheduled, in-progress, completed)
  - Quick "Start Consultation" button

- **Pending Consultations Card**
  - List of consultations awaiting completion
  - Patient details, issue summary
  - Action button to continue

- **Patient Indicators Card**
  - Recent patient admissions (last 7 days)
  - Pending surgeries
  - Hospitalized patients count

- **Requests Sent Card**
  - Appointment requests sent to receptionist (pending/approved/rejected)
  - Surgery indications sent

- **Lab Status Card**
  - Lab requests sent (pending/completed)
  - Results available notifications

- **Hospitalization Context Card**
  - Currently hospitalized patients
  - Room/bed assignments
  - Quick links to patient records

**Technical Implementation:**
- Single API call to fetch all dashboard data (aggregated)
- Cache dashboard data for 5 minutes
- Refresh button for immediate update

---

### 2.2 Today's Appointments Page
**Features:**
- Table view with columns: Time, Patient Name, Type, Status, Actions
- Filter options:
  - Time range (morning, afternoon, evening)
  - Status (scheduled, in-progress, completed)
  - Consultation type
- Sort by: Time, Patient Name, Status
- Quick action buttons:
  - Start Consultation (if appointment is today + within 15-min window)
  - View Patient Details
  - Reschedule Appointment (if not started)
  - Cancel Appointment (if not started)
  - Mark as Completed (after consultation)

**Business Logic:**
- Start button enabled ONLY if:
  - Appointment date is TODAY
  - Current time is within appointment start time ± buffer (e.g., 15 minutes before)
  - Appointment status is SCHEDULED
  - No conflicting consultation in progress

---

### 2.3 Appointment Start Logic with Auto-Cancel
**Rules:**
1. Doctor clicks "Start Consultation" button
2. System validates:
   - Is appointment scheduled for today? (Date = TODAY)
   - Is time within valid window? (NOW <= AppointmentTime + 15 min)
   - Is appointment status SCHEDULED?
3. If valid: Update appointment status to IN_PROGRESS, start consultation
4. If invalid: Show error message

**Auto-Cancel After 15 Minutes:**
- Scheduled job runs every minute
- Checks for appointments where:
  - status = SCHEDULED
  - appointment_date = TODAY
  - appointment_time + 15 min < NOW
- Updates status to CANCELLED
- Disables Start button immediately

**Implementation:**
- Database: Add `started_at`, `completed_at` timestamps
- Scheduled task (Spring Scheduler or Quartz)
- Real-time button state update in frontend

---

### 2.4 Doctor Appointment Request System
**Current Problem:**
- Doctors can directly create appointments (not logical)

**Desired Flow:**
1. Doctor views patient → clicks "Request Appointment"
2. Form: Patient name, preferred date range, reason, urgency
3. Request sent to receptionist inbox
4. Receptionist reviews and plans actual appointment
5. Doctor-requested patients get priority in scheduling

**Implementation:**
- Remove "Create Appointment" button from doctor UI
- Add "Request Appointment" button in patient view
- `appointment_requests` table (doctor_id, patient_id, preferred_dates, reason, urgency, status, created_at)
- Receptionist inbox shows appointment requests with filters (pending, approved, rejected)
- Email notification to receptionist on new request

---

### 2.5 Lab Request Workflow (Fix Broken Flow)
**Current Issues:**
- Request not properly reaching lab employee
- Not displayed correctly
- Process doesn't continue to execution and result upload

**Desired Flow:**
1. Doctor: Fills lab request form (patient, test types, urgency, notes)
2. Lab Employee: Receives notification + sees request in inbox
3. Lab Employee: Reviews request, adds to schedule, marks as "In Progress"
4. Lab Employee: Uploads test results/reports after execution
5. Patient: Can view results in medical record
6. Doctor: Receives notification when results available

**Implementation:**
- `lab_requests` table (doctor_id, patient_id, test_types, urgency, status, created_at)
- `lab_request_results` table (lab_request_id, result_file, uploaded_by, uploaded_at)
- Lab employee dashboard showing pending/in-progress/completed requests
- Real-time notifications to doctor when results available
- Receptionist can see minimal details (patient, test type, urgency) for patient guidance

---

### 2.6 Prescription Workflow (Currently Missing)
**Flow:**
1. Doctor: Creates prescription (patient, medications, dosage, frequency, duration, notes)
2. Pharmacist: Receives notification, sees in pending prescriptions
3. Pharmacist: Fulfills prescription, marks as completed
4. Guardian: Notified prescription is ready
5. Guardian: Can view prescription details, print if needed

**Implementation:**
- `prescriptions` table (doctor_id, patient_id, medication, dosage, frequency, duration, status, created_at)
- Notification to pharmacist on creation
- Pharmacist UI: Prescription queue with filters (pending, fulfilled, expired)
- API endpoint for guardian to view prescriptions

---

### 2.7 Hospitalization-Related Needs
**Doctor Capabilities:**
- View hospitalized patients assigned to them
- View patient's room and bed assignment
- Add/update follow-up instructions
- Create discharge follow-up list (see Tier 5)

**Implementation:**
- `hospitalizations` table (patient_id, room_number, bed_number, admitted_date, discharge_date)
- Doctor can query hospitalized patients
- Link room/bed info to patient record

---

### 2.8 Discharge Follow-Up List
**Flow:**
1. At discharge, doctor creates follow-up list for guardian
2. Includes: Medications to give, timing, care instructions, recommendations, monitoring items
3. Guardian receives list, can view and print
4. System monitors compliance, sends reminders

**Implementation:**
- `discharge_follow_ups` table (patient_id, doctor_id, created_at, due_date)
- `follow_up_items` table (follow_up_id, item_type, description, frequency, status)
- Guardian app shows follow-up list with checklist
- (Tier 5) Compliance monitoring and alert triggering

---

### 2.9 Surgery Indication
**Flow:**
1. Doctor determines patient needs surgery during/after consultation
2. Doctor clicks "Indicate Surgery Needed"
3. Form: Urgency level (routine, semi-urgent, urgent, emergency), notes
4. Sent to receptionist for planning
5. Receptionist schedules operation (see Tier 4)

**Implementation:**
- `surgery_indications` table (doctor_id, patient_id, urgency, notes, created_at)
- Notification to receptionist on creation
- Receptionist can view pending surgery indications with filters

---

## TIER 3: RECEPTIONIST & APPOINTMENT MANAGEMENT (Weeks 15-20)

### 3.1 Receptionist Dashboard
**Widgets:**
- **Appointments Today:** Count + upcoming appointments in next 2 hours
- **Pending Appointment Requests:** Count of requests from doctors waiting to be scheduled
- **Messages:** Unread message count
- **Surgery Indications:** Pending surgery requests awaiting planning
- **Room Occupancy:** Current patients, room/bed status
- **Patient Arrivals:** Patients checking in today

---

### 3.2 Appointment Modification Logic
**Scenario 1: Guardian requests specific date**
1. Receptionist receives modification request
2. Checks if requested date is available:
   - No other appointments on that date
   - Doctor is available on that date
3. If available: Approve and update appointment
4. If unavailable: Reject with reason

**Scenario 2: Guardian wants earlier/later (no specific date)**
1. Receptionist manually searches for available slot:
   - Find earliest available date after current appointment
   - Verify doctor availability
2. Suggests to guardian (or chooses best option)
3. Updates appointment if guardian accepts

**Database:**
- Update `appointments` table with modification request status
- Track: original date, requested date, modified date, modified_by

---

### 3.3 Room & Bed Assignment
**Flow:**
1. Patient is admitted, receptionist assigns room and bed
2. Form: Select room from dropdown, select bed from room availability
3. Save to patient record (visible to doctor, nurse, staff)
4. Update room occupancy status

**Implementation:**
- `rooms` table (room_number, capacity, location)
- `beds` table (room_id, bed_number, occupied_flag)
- `hospitalizations` table includes room_id, bed_number
- API to get available beds in a room

---

### 3.4 Appointment Archiving
**Flow:**
1. Cancelled appointments → moved to archive (status = ARCHIVED)
2. Completed appointments → option to archive after 30 days
3. Archived appointments: Not visible in main view, searchable in archive view

**Implementation:**
- Add `is_archived` flag to appointments
- Archive view with date range filter
- Scheduled job to auto-archive completed appointments after 30 days

---

## TIER 4: SPECIALIZED WORKFLOWS (Weeks 21-28)

### 4.1 Surgery/Procedure Workflow
**Receptionist Planning:**
1. Receptionist views pending surgery indications
2. Creates operation plan:
   - Select available surgery date
   - Select available time slot
   - Select surgeon from available surgeons
   - Confirm operation room availability
   - Urgency level automatically set from indication
3. Schedule is optimized by urgency (more urgent = earlier slot)

**Surgeon Dashboard:**
- **Today's Operations:** Card showing operations scheduled for today
- **Upcoming Operations:** List of operations in next 7 days
- **Operation Details:** Patient info, urgency, pre-op requirements

**Surgeon Capabilities:**
- View operation details (read-only patient info)
- Request pre-operation lab tests
- Generate pre-operation report (patient history, prep done)
- Generate post-operation report (actions taken, observations, clinical notes)
- Create follow-up to-do lists for nurses

**Implementation:**
- `surgeries` table (patient_id, surgeon_id, date, time, urgency, status)
- `surgery_reports` table (surgery_id, report_type (pre/post), content, created_by, created_at)
- `surgery_follow_ups` junction table linking surgeries to nurse follow-up tasks

---

### 4.2 Nurse Role Completion
**Dynamic Dashboard:**
- To-do lists assigned (status badges: pending, in-progress, completed)
- Dialysis sessions (today's sessions, upcoming)
- Patient assignments (hospitalized patients)

**To-Do List Features:**
- Display all tasks assigned to nurse
- Mark as in-progress, then completed
- Add notes and signature for each completed task
- View task source (doctor, surgeon)

**Dialysis Session Documentation:**
- Separate form (NOT part of general to-do system)
- Fields: Blood pressure (start/end), session observations, patient condition (before/during/after), dialysis indicators, complications notes
- Mandatory signature before completing session
- Timed: Records start and end time automatically

**Implementation:**
- `nurse_to_dos` table (nurse_id, assignee_id, assigned_by, task_description, status, completed_at, signature)
- `dialysis_sessions` table (patient_id, nurse_id, start_time, end_time, duration, session_data (JSON), signature, created_at)
- Signature field stores digital signature or nurse initials

---

### 4.3 Lab Agent Dashboard & Report Upload
**Components:**
- Pending lab requests (from doctors)
- In-progress tests
- Results ready for upload
- Completed tests (archive)

**Report Upload:**
- Select patient
- Select lab request
- Upload machine-generated report (PDF/image)
- Save to lab_request_results table

**Implementation:**
- `lab_reports` table (lab_request_id, file_path, uploaded_by, uploaded_at)
- File storage (S3 or local storage)
- API endpoint for guardian to access results

---

### 4.4 Pharmacy System
**Access Control:**
- Pharmacy section only visible to pharmacist
- Remove from all other user UIs

**Three Stock Types:**
1. **Medication Stock:** Medicines for patients
2. **Medical Equipment:** Syringes, protective items, etc.
3. **Dialysis Materials:** Blood bags, dialysis supplies

**Stock Management:**
- For each stock type: Display quantities, set alerts (low stock threshold), list suppliers
- Add stock: Record requested quantity, received quantity (supplier delivery)
- Remove stock: Medical staff requests → Pharmacist records what was taken → Quantity auto-deducted

**Implementation:**
- `stock_types` table (name: MEDICATION | EQUIPMENT | DIALYSIS)
- `stocks` table (stock_type, item_name, quantity, low_threshold, supplier_id)
- `stock_transactions` table (stock_id, transaction_type (add/remove), quantity, requested_by, created_at)
- `suppliers` table (name, contact, items_supplied)

---

## TIER 5: GUARDIAN & HOME CARE (Weeks 29-34)

### 5.1 Guardian Document Upload
**Uploadable Documents:**
- Lab test results (outside the system)
- Prescriptions (from external providers)
- Dialysis session reports (external clinics)
- Other medical documents

**Implementation:**
- `guardian_uploads` table (patient_id, file_name, file_path, upload_date, document_type)
- File storage
- Visible in patient's medical record

---

### 5.2 Guardian Printing
**Printable Documents:**
- Lab results
- Prescriptions
- Dialysis reports
- Follow-up lists
- Medical summaries

**Implementation:**
- Print-friendly HTML view for each document type
- Browser print (Ctrl+P) functionality
- Optional: Server-side PDF generation

---

### 5.3 Follow-Up List After Discharge
**Creation:**
1. Doctor creates list at discharge time
2. Items: Medications (with timing), care instructions, activity restrictions, follow-up appointments, monitoring requirements
3. Guardian receives list (email + in-app notification)

**Guardian View:**
- Checklist format for medications/tasks
- Clear instructions for each item
- Printable format
- Set reminders for medications

**Implementation:**
- `discharge_follow_ups` table
- `follow_up_items` table (item_type, description, frequency, start_date, end_date)
- Guardian app shows checklist with completion status

---

### 5.4 Home Follow-Up Supervision & Compliance Monitoring
**Monitoring:**
1. Guardian marks completion of follow-up items (medication taken, activity done)
2. System tracks compliance
3. If non-compliant (missed medication, late task): System alerts doctor + sends reminder to guardian
4. If critical non-compliance (missed critical medication multiple times): System sends urgent notification to bring child back to hospital

**Alerts:**
- SMS/WhatsApp reminders 1 hour before medication time
- Email summary of daily compliance
- Critical alert if compliance < 70% for critical medications

**Implementation:**
- `follow_up_tracking` table (follow_up_item_id, completion_date, completed_by (guardian))
- Job scheduler to check compliance daily
- Rules engine for alert triggering (critical items, compliance percentage)
- SMS/WhatsApp integration (Twilio or similar)

---

## TIER 6: ADVANCED FEATURES & ANALYTICS (Weeks 35-40)

### 6.1 Statistics & Analytics
**Metrics:**
- Total appointments (today, this month, all time)
- Appointment completion rate
- Average consultation duration
- Doctor workload (appointments per doctor)
- Hospital occupancy rate
- Lab tests completed
- Surgery success rate (outcomes tracking)
- Staff performance (tasks completed, average response time)
- Patient satisfaction (if surveys implemented)

**Filtering:**
- By date range
- By department/doctor
- By patient demographics
- By status/outcome

**Reports:**
- Generate custom reports
- Export to CSV/PDF
- Scheduled email reports (weekly/monthly)

**Implementation:**
- Database views for aggregated metrics
- Analytics service layer
- Chart library (Chart.js, ApexCharts) for visualization

---

### 6.2 Staff Internal Messaging
**Features:**
- One-to-one messaging (staff to staff)
- Group messaging (departments)
- Real-time chat (WebSocket)
- Message history
- Search messages
- Typing indicators
- Read receipts

**Restrictions:**
- Only visible to staff accounts
- Not available to guardians
- Role-based channel visibility (e.g., "Surgery Team" only for surgeons)

**Implementation:**
- `messages` table (sender_id, recipient_id, channel_id, content, created_at)
- `message_channels` table (name, members)
- WebSocket server for real-time updates
- Message encryption for privacy

---

### 6.3 Clinic Resources Visualization
**Current:** Text-based resource management
**Desired:** Visual representation of clinic layout

**Options:**
1. **2D Floor Plan:** Interactive map showing rooms, beds, equipment
2. **3D Visualization:** 3D model of clinic layout (more complex, lower priority)

**Features:**
- View room availability
- Click room to see details
- Drag-and-drop bed assignment
- Real-time occupancy updates

**Implementation:**
- SVG-based 2D floor plan
- Interactive click handlers
- Overlay current occupancy data

---

## Cross-Cutting Concerns

### Error Handling & Validation
- Input validation on all forms
- API error responses with clear messages
- User-friendly error notifications
- Logging of errors for debugging

### Performance Optimization
- Database indexing on frequently queried columns
- Caching strategies (Redis for notifications, dashboard data)
- Pagination for large data sets
- Lazy loading for images and documents

### Security
- JWT token expiration and refresh
- HTTPS enforcement
- SQL injection prevention (parameterized queries)
- CORS configuration
- File upload validation (type, size)
- Rate limiting on sensitive endpoints

### Testing Strategy
- Unit tests for business logic
- Integration tests for API endpoints
- E2E tests for critical workflows (login, appointment creation, lab request)
- Test data seeding for development

---

## Resource Allocation Recommendations

**Backend (Java/Spring Boot):**
- 1 Senior Developer (architecture, authentication, complex workflows)
- 2 Mid-Level Developers (API endpoints, database)
- 1 Junior Developer (support, simple endpoints)

**Frontend (Angular):**
- 1 Senior Developer (architecture, complex UIs)
- 2 Mid-Level Developers (components, pages)
- 1 Junior Developer (styling, simple components)

**Database/DevOps:**
- 1 Database Admin (schema design, optimization, backups)
- 1 DevOps Engineer (deployment, server management, monitoring)

**QA:**
- 1 QA Lead (test planning, automation framework)
- 2 QA Testers (manual testing, bug reporting)

---

## Timeline Summary
- **Weeks 1-6:** Foundation (login, navigation, permissions, notifications) = ~25% effort
- **Weeks 7-14:** Doctor workflows (dashboards, appointments, requests) = ~30% effort
- **Weeks 15-20:** Receptionist & appointments = ~20% effort
- **Weeks 21-28:** Surgery, nursing, pharmacy = ~20% effort
- **Weeks 29-34:** Guardian features = ~10% effort
- **Weeks 35-40:** Analytics & messaging = ~10% effort

**Total: 40 weeks (~10 months) for full implementation with 8 developers**

---

Created: April 28, 2026
Status: READY FOR TEAM REVIEW
Next Step: Assign tiers to development teams, create detailed task cards for Tier 1
