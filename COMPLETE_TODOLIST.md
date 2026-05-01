# Complete Project Todolist - NephroPaidi Platform

## Executive Summary
**Total Requirements**: 60+ distinct features across 15 role/feature areas  
**Already Completed**: 6 core features (doctor dashboard, lab requests, surgery indication, discharge follow-up, appointment start logic, today appointments)  
**Remaining Work**: 54+ features organized in 4 priority tiers and 8 feature tracks  
**Estimated Total Effort**: 200-250 hours (8-10 weeks for 2-3 developers)

---

## Phase 0: Foundation (ALREADY COMPLETED) ✅

| # | Feature | Role | Status | Notes |
|---|---------|------|--------|-------|
| ✅ P0.1 | Doctor Dashboard | Doctor | DONE | Real-time metrics (appointments, requests, labs, surgeries) |
| ✅ P0.2 | Lab Request Workflow (Fixed) | Doctor/Lab | DONE | Create → Upload → Track |
| ✅ P0.3 | Surgery Indication | Doctor | DONE | Doctor triggers with urgency |
| ✅ P0.4 | Discharge Follow-Up | Doctor/Patient | DONE | Create follow-up instructions with dynamic items |
| ✅ P0.5 | Appointment Start Logic | Doctor | DONE | 15-min window validation, auto-cancel after 15min |
| ✅ P0.6 | Today Appointments Display | Doctor | DONE | Shows scheduled appointments, start button |

---

## Phase 1: CRITICAL PATH (Authentication & Core Infrastructure)

### TIER 1A: Login & Authentication System
**Priority**: 🔴 BLOCKING | **Effort**: 20-25 hours | **Dependencies**: None

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **1A.1** | Home Page CMS | Public/Admin | ⏳ TODO | Admin interface to manage public home page content dynamically (titles, images, sections, announcements) WITHOUT code changes | 12 |
| **1A.2** | Remember Me | Frontend/Backend | ⏳ TODO | Persistent login token (cookies + browser storage), configurable timeout | 4 |
| **1A.3** | Forgot Password | Frontend/Backend | ⏳ TODO | Email-based password reset flow, secure token generation, email integration | 6 |
| **1A.4** | Two-Factor Authentication (2FA) | Backend | ⏳ TODO | 2FA for Admin/HR/Doctor/Staff roles. SMS or TOTP. Keycloak or custom implementation | 10 |

**Prerequisites**: None  
**Unlocks**: All authenticated workflows

---

### TIER 1B: Role-Based Access Control & Permissions
**Priority**: 🔴 BLOCKING | **Effort**: 15-18 hours | **Dependencies**: 1A.4

| # | Feature | Role | Status | Requirements | Est. Hours |
|---|---------|------|--------|--------------|-----------|
| **1B.1** | HR Supervision & Permissions | HR | ⏳ TODO | HR supervises all staff EXCEPT Admin. Can add/update/delete contracts. Cannot modify Admin. | 6 |
| **1B.2** | Admin Full Supervision | Admin | ⏳ TODO | Admin supervises all staff + HR. Full oversight but NOT operational HR contract execution. Supervisory role only. | 5 |
| **1B.3** | Receptionist Permissions | Receptionist | ⏳ TODO | Appointment management, lab request visibility (minimal), staff messaging, dashboard access | 4 |
| **1B.4** | Pharmacy Access Restriction | Pharmacy | ⏳ TODO | Pharmacy module visible ONLY to pharmacist role. Not visible to other users. | 3 |

**Prerequisites**: Login system (1A)  
**Unlocks**: Role-specific features (Receptionist, HR, Admin dashboards)

---

## Phase 2: SECONDARY FEATURES (Role-Specific Workflows)

### TIER 2A: Receptionist Module
**Priority**: 🟠 HIGH | **Effort**: 25-30 hours | **Dependencies**: 1B.3

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **2A.1** | Receptionist Dashboard | Frontend/Backend | ⏳ TODO | Statistics: total appointments, pending messages, pending appointment requests, lab requests, operational counts. Real-time metrics. | 6 |
| **2A.2** | Appointment Modification | Backend | ⏳ TODO | Receptionist modifies appointment date/time IF: (1) date available, (2) no conflicting appointments, (3) doctor available. Validation logic required. | 8 |
| **2A.3** | Appointment Archiving | Backend | ⏳ TODO | Cancelled/Approved appointments move to archive. Search/filter archive. Soft delete or archive table. | 4 |
| **2A.4** | Lab Request Visibility (Minimal) | Frontend/Backend | ⏳ TODO | Receptionist sees lab requests with ONLY minimal details: patient ID, test type, urgency. NO full medical details. Guides patient to lab location. | 4 |
| **2A.5** | Room/Bed Assignment | Frontend/Backend | ⏳ TODO | Receptionist assigns room number + bed number during hospitalization. Saves in patient info. Visible to doctor/nurse. | 5 |
| **2A.6** | Appointment Request Inbox | Frontend | ⏳ TODO | Receptionist receives doctor appointment requests. Approves/rejects with feedback. Creates actual appointment after approval. Priority flag for doctor-requested patients. | 6 |

**Prerequisites**: Receptionist role (1B.3), Doctor appointment requests (2D.3)  
**Unlocks**: Complete receptionist workflow

---

### TIER 2B: Doctor Module - Enhancements
**Priority**: 🟠 HIGH | **Effort**: 20-25 hours | **Dependencies**: 2D (see below)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **2B.1** | Doctor Appointment Requests | Frontend/Backend | ⏳ TODO | Doctor STOPS creating appointments directly. Instead sends request to receptionist with: patient name, preferred date range, reason. Receptionist schedules. | 8 |
| **2B.2** | Prescription Sending | Frontend/Backend | ⏳ TODO | Doctor creates prescription (drugs with dosage/frequency/duration). Sent to pharmacy. Pharmacist receives in inbox. | 7 |
| **2B.3** | Hospitalization Todo Lists | Frontend/Backend | ⏳ TODO | Doctor creates to-do lists for nurses after hospitalization. Includes display in nurse platform + nurse signature on completion. (See Nurse 3B.2) | 8 |
| **2B.4** | Today Appointments Page Polish | Frontend | ⏳ TODO | Group by time (Morning/Afternoon/Evening). Show countdown timer to 15-min window. Status badges. Color coding by urgency. | 4 |

**Prerequisites**: Appointment requests implemented (2D.3), Nurse signatures (3B.2)  
**Unlocks**: Proper doctor workflow (no direct scheduling)

---

### TIER 2C: Pharmacy Module
**Priority**: 🟠 HIGH | **Effort**: 30-35 hours | **Dependencies**: 1B.4

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **2C.1** | Three Stock Types | Backend/Frontend | ⏳ TODO | Create 3 separate stock inventories: (1) Medications, (2) Medical equipment (syringes, etc), (3) Dialysis materials (blood bags, etc). Each with quantity tracking. | 8 |
| **2C.2** | Stock Alerts & Suppliers | Backend/Frontend | ⏳ TODO | Low-stock alerts. Supplier management. For each stock type, track: what was ordered, what was delivered. | 6 |
| **2C.3** | Outgoing Stock Control | Backend/Frontend | ⏳ TODO | When medical staff request dialysis/equipment items, pharmacist logs: which items, quantities. Auto-deduct from total stock. Traceability required. | 8 |
| **2C.4** | Pharmacy Inbox | Frontend/Backend | ⏳ TODO | Pharmacist receives prescriptions from doctors. Marks as in-progress/completed. Notes field. | 4 |
| **2C.5** | Supplier Delivery Tracking | Frontend/Backend | ⏳ TODO | Pharmacist records supplier deliveries: requested qty vs actual delivered qty. Tracks discrepancies. | 4 |

**Prerequisites**: Pharmacy access restriction (1B.4), Prescription system (2B.2)  
**Unlocks**: Complete pharmacy workflow

---

### TIER 2D: Appointment Request Workflow
**Priority**: 🟠 HIGH | **Effort**: 15-20 hours | **Dependencies**: 1B.3

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **2D.1** | AppointmentRequest Entity | Backend | ⏳ TODO | New entity: doctor_id, patient_id, requested_date, reason, urgency, status (PENDING/APPROVED/REJECTED/SCHEDULED), receptionist_notes | 2 |
| **2D.2** | Flyway Migration V12 | Backend | ⏳ TODO | Create appointment_requests table with proper indexes and foreign keys | 1 |
| **2D.3** | Doctor Appointment Request Form | Frontend | ⏳ TODO | Doctor selects patient, date preference, reason. Sends request (no direct scheduling). | 5 |
| **2D.4** | Receptionist Approval Workflow | Backend/Frontend | ⏳ TODO | Receptionist inbox for requests. Approve (creates appointment) / Reject (with reason). Validation logic for date/doctor availability. | 8 |
| **2D.5** | Priority Flagging | Backend | ⏳ TODO | Doctor-requested appointments marked as HIGH priority vs regular appointments. | 2 |

**Prerequisites**: Receptionist role (1B.3)  
**Unlocks**: Receptionist workflow (2A.6), Doctor appointment requests (2B.1)

---

## Phase 3: SPECIALIZED WORKFLOWS (Medical Features)

### TIER 3A: Nurse Module
**Priority**: 🟠 HIGH | **Effort**: 25-30 hours | **Dependencies**: None (parallel work)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **3A.1** | Nurse Dashboard | Frontend/Backend | ⏳ TODO | Dynamic dashboard with: assigned patients, pending to-do lists, dialysis sessions today, operations scheduled. Real-time updates. | 7 |
| **3A.2** | To-Do List Display | Frontend/Backend | ⏳ TODO | Display doctor/surgeon to-do lists assigned to nurse. Checkbox for completion. (Linked to 3A.3 - signatures) | 5 |
| **3A.3** | Nurse Signatures | Frontend/Backend | ⏳ TODO | When nurse completes task from to-do list, save: completion timestamp + digital signature. Traceability required. | 6 |
| **3A.4** | Dialysis Session Documentation Form | Frontend/Backend | ⏳ TODO | Post-dialysis form (separate from to-do lists): blood pressure, observations, patient condition before/during/after, dialysis indicators, complications notes. Structured fields. | 8 |
| **3A.5** | Dialysis Session Time Tracking | Frontend/Backend | ⏳ TODO | Timer start/stop for dialysis sessions. Save duration automatically. Visible to nurse during session. | 4 |

**Prerequisites**: None (but requires to-do system from Doctor/Surgeon)  
**Unlocks**: Complete nurse workflow, dialysis tracking

---

### TIER 3B: Lab Agent Module
**Priority**: 🟡 MEDIUM | **Effort**: 10-12 hours | **Dependencies**: ✅ Lab Request (already done)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **3B.1** | Lab Agent Dashboard | Frontend/Backend | ⏳ TODO | List pending lab requests for all patients. Status tracking. | 5 |
| **3B.2** | Machine Test Report Upload | Frontend/Backend | ⏳ TODO | Lab agent uploads machine-generated test reports for each patient. Attach to lab request. Mark request as COMPLETED. | 6 |

**Prerequisites**: Lab request system (✅ already done)  
**Unlocks**: Complete lab workflow

---

### TIER 3C: Surgery / Procedure Workflow
**Priority**: 🟠 HIGH | **Effort**: 30-35 hours | **Dependencies**: 2A (Receptionist), Surgery Indication (✅ done)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **3C.1** | Surgery Planning (by Receptionist) | Backend/Frontend | ⏳ TODO | Receptionist receives surgery indications from doctor. Plans: date, time, surgeon (check availability), urgency priority, OR availability. | 8 |
| **3C.2** | Surgeon Dashboard | Frontend/Backend | ⏳ TODO | Show scheduled operations for today + upcoming. Full patient medical details (read-only, cannot modify). | 6 |
| **3C.3** | Pre-Surgery Report | Frontend/Backend | ⏳ TODO | Surgeon generates pre-surgery report: patient context, preparation done, clinical observations. Save to system. | 5 |
| **3C.4** | Post-Surgery Report | Frontend/Backend | ⏳ TODO | Surgeon generates post-surgery report: actions taken, steps performed, observations after, clinical notes. Save with full detail. | 5 |
| **3C.5** | Surgery To-Do Lists for Nurses | Frontend/Backend | ⏳ TODO | Surgeon creates to-do lists for nurses for post-op care. Sent to nurse platform. (See 3A.2, 3A.3 for nurse implementation) | 4 |
| **3C.6** | Lab Request from Surgeon | Backend/Frontend | ⏳ TODO | Surgeon can request lab tests before surgery if needed. Integrated with lab system. | 3 |

**Prerequisites**: Receptionist workflow (2A), Surgery indication (✅ done), Nurse to-do system (3A.2)  
**Unlocks**: Complete surgery workflow

---

## Phase 4: COMMUNICATION & MONITORING

### TIER 4A: Internal Staff Messaging
**Priority**: 🟠 HIGH | **Effort**: 25-30 hours | **Dependencies**: 1B (Role permissions)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **4A.1** | Staff Messenger Backend | Backend | ⏳ TODO | New service: staff-only messaging. One-to-one + group chats (optional). Message persistence. Role-based access. | 10 |
| **4A.2** | Messenger Frontend Component | Frontend | ⏳ TODO | Chat UI (Facebook Messenger style). Inbox with conversations. Real-time notifications. | 8 |
| **4A.3** | Message Archiving & Search | Backend/Frontend | ⏳ TODO | Search message history. Archive conversations. | 4 |
| **4A.4** | Typing Indicators & Read Receipts | Frontend/Backend | ⏳ TODO | Show "someone is typing" + read status indicators. | 3 |

**Prerequisites**: Role-based access (1B)  
**Unlocks**: Internal communication system

---

### TIER 4B: Notifications System (Application-Wide)
**Priority**: 🟠 HIGH | **Effort**: 20-25 hours | **Dependencies**: 1B (Roles)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **4B.1** | Role-Based Notification Engine | Backend | ⏳ TODO | Create notifications based on role. Doctor gets appointment alerts, Receptionist gets requests, etc. Filter by role. | 8 |
| **4B.2** | Notification UI Widget | Frontend | ⏳ TODO | Notification bell icon (top navbar). Popup list showing recent notifications. | 5 |
| **4B.3** | Notification Center | Frontend | ⏳ TODO | Full page to view all notifications, search, filter, mark as read, delete. | 6 |
| **4B.4** | Operational Alerts | Backend | ⏳ TODO | NOT just generic alerts. Support workflow-specific notifications: "Appointment in 15 min", "Prescription pending", "Lab results ready", etc. | 6 |

**Prerequisites**: Role permissions (1B)  
**Unlocks**: Application-wide notification system

---

## Phase 5: GUARDIAN & PATIENT FEATURES

### TIER 5A: Guardian Upload Features
**Priority**: 🟡 MEDIUM | **Effort**: 12-15 hours | **Dependencies**: None (parallel)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **5A.1** | Lab Result Upload (Guardian) | Frontend/Backend | ⏳ TODO | Guardian uploads external lab results. Attach to patient file. | 4 |
| **5A.2** | Prescription Upload (Guardian) | Frontend/Backend | ⏳ TODO | Guardian uploads prescription documents. Attach to patient file. | 3 |
| **5A.3** | Dialysis Report Upload (Guardian) | Frontend/Backend | ⏳ TODO | Guardian uploads dialysis session reports. Attach to patient file. | 4 |
| **5A.4** | Document Printing | Frontend | ⏳ TODO | Guardian can print: lab results, prescriptions, dialysis reports, follow-up documents. Simple print-to-PDF. | 4 |

**Prerequisites**: None  
**Unlocks**: Guardian document management

---

### TIER 5B: Discharge Follow-Up (Home Care Monitoring)
**Priority**: 🟠 HIGH | **Effort**: 25-30 hours | **Dependencies**: ✅ Discharge Follow-Up (done)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **5B.1** | Home Follow-Up List Creation | Frontend/Backend | ⏳ TODO | Doctor creates follow-up list for guardian at discharge: medications, timing, instructions, monitoring items, post-discharge actions. (Builds on ✅ Discharge Follow-Up) | 6 |
| **5B.2** | Guardian Follow-Up Tracking | Frontend/Backend | ⏳ TODO | System monitors if guardian is following instructions on time. Track completion status. | 8 |
| **5B.3** | Reminder Notifications | Backend | ⏳ TODO | Send reminders to guardian via SMS/WhatsApp for medications/follow-ups. | 6 |
| **5B.4** | Compliance Alerts | Backend | ⏳ TODO | If guardian misses scheduled follow-ups, system generates alerts to doctor/system. Risk assessment. | 5 |
| **5B.5** | Emergency Escalation | Backend | ⏳ TODO | If non-compliance risk is HIGH, system sends urgent notification to guardian to bring child back to hospital. | 4 |

**Prerequisites**: ✅ Discharge Follow-Up (done)  
**Unlocks**: Complete home care monitoring workflow

---

## Phase 6: ADMINISTRATION & ANALYTICS

### TIER 6A: Analytics & Statistics
**Priority**: 🟠 HIGH | **Effort**: 25-30 hours | **Dependencies**: 1B (Admin/HR roles)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **6A.1** | Dynamic Statistics Dashboard | Frontend/Backend | ⏳ TODO | Replace static stats with historical, time-series data. NOT real-time only. Charts, filtering by date range. | 8 |
| **6A.2** | Staff Performance Statistics | Backend/Frontend | ⏳ TODO | Admin sees ALL staff performance metrics. HR sees staff performance but ONLY for their reports. Admin > HR hierarchy maintained. | 10 |
| **6A.3** | Appointment Statistics | Frontend/Backend | ⏳ TODO | No-show rate, cancellation rate, avg wait time, doctor utilization, etc. | 4 |
| **6A.4** | Lab Statistics | Frontend/Backend | ⏳ TODO | Request volume, turnaround time, completion rate, pending count. | 3 |

**Prerequisites**: Admin/HR roles (1B)  
**Unlocks**: Management dashboards

---

### TIER 6B: Audit Logs & User Guidance
**Priority**: 🟡 MEDIUM | **Effort**: 20-25 hours | **Dependencies**: 1B

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **6B.1** | Audit Logs Redesign | Frontend/Backend | ⏳ TODO | Reorganize audit logs: better layout, filtering, searchable, organized by action type. NOT hard to read. | 8 |
| **6B.2** | Application User Guidance | Frontend | ⏳ TODO | Tooltips, contextual help, onboarding flow, guided tours for first-time users. Self-explanatory interface. | 8 |
| **6B.3** | Role-Based Help Resources | Frontend | ⏳ TODO | Different help content for different roles. Doctor sees doctor-specific guidance, etc. | 4 |
| **6B.4** | Video Tutorials (Optional) | Frontend | ⏳ TODO | Optional: embedded video tutorials for complex workflows. | 5 |

**Prerequisites**: 1B (Admin access)  
**Unlocks**: Better usability & compliance

---

### TIER 6C: Clinic Resources Management
**Priority**: 🟡 MEDIUM | **Effort**: 15-20 hours | **Dependencies**: None

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **6C.1** | Clinic Resources Module | Frontend/Backend | ⏳ TODO | Improved design & organization of clinic resources (rooms, equipment, etc). | 6 |
| **6C.2** | 2D Visualization (Optional) | Frontend | ⏳ TODO | Optional: 2D floor map of clinic showing rooms, beds, equipment locations. Interactive. | 8 |
| **6C.3** | 3D Visualization (Nice-to-Have) | Frontend | ⏳ TODO | Optional: 3D clinic layout (low priority, high complexity). Only if budget allows. | 10 |

**Prerequisites**: None  
**Unlocks**: Better resource management

---

## Phase 7: SECURITY & PERSONALIZATION

### TIER 7A: User Settings & Profile Management
**Priority**: 🟡 MEDIUM | **Effort**: 15-18 hours | **Dependencies**: 1A (Login)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **7A.1** | Profile Information Editing | Frontend/Backend | ⏳ TODO | Allow users to edit: name, email, phone, specialization, department. With validation. | 4 |
| **7A.2** | Language Settings | Frontend/Backend | ⏳ TODO | Implement multi-language support. User can select language (Arabic/French/English). Apply site-wide. | 6 |
| **7A.3** | Password Change | Frontend/Backend | ⏳ TODO | Secure password change flow. Old password verification. | 3 |
| **7A.4** | Notification Preferences | Frontend/Backend | ⏳ TODO | User controls which notifications they receive. Opt-in/out per notification type. | 4 |
| **7A.5** | Theme/Interface Preferences | Frontend | ⏳ TODO | Optional: light/dark theme toggle, font size, layout preferences. | 3 |

**Prerequisites**: Login system (1A)  
**Unlocks**: Personalized user experience

---

## Phase 8: UI/UX IMPROVEMENTS

### TIER 8A: Navigation & Layout
**Priority**: 🟡 MEDIUM | **Effort**: 18-22 hours | **Dependencies**: None (parallel design work)

| # | Feature | Component | Status | Requirements | Est. Hours |
|---|---------|-----------|--------|--------------|-----------|
| **8A.1** | Left Sidebar Redesign | Frontend | ⏳ TODO | Cleaner, hierarchical navigation. Grouped sections. Role-based menu items visible. Better visual hierarchy. | 10 |
| **8A.2** | Responsive Layout Fix | Frontend | ⏳ TODO | Fix refresh/display issues on pages. Information updates without requiring manual refresh. Real-time UI updates. | 6 |
| **8A.3** | Navigation Bar Polish | Frontend | ⏳ TODO | Top navbar: logo, search, notifications, user menu. Professional appearance. | 6 |

**Prerequisites**: None  
**Unlocks**: Better UX for all roles

---

## Dependency Graph & Execution Order

```
PHASE 0 (✅ DONE)
├─ Doctor Dashboard ✅
├─ Lab Requests ✅
├─ Surgery Indication ✅
├─ Discharge Follow-Up ✅
├─ Appointment Start Logic ✅
└─ Today Appointments ✅

PHASE 1 (CRITICAL - MUST START)
├─ 1A: Login & Auth (Home CMS, Remember Me, Forgot Password, 2FA)
│  └─ Unlocks: All authenticated workflows
└─ 1B: Role-Based Access (HR/Admin/Receptionist/Pharmacy)
   └─ Unlocks: Phase 2 (Receptionist, Pharmacy, Appointments)

PHASE 2 (SECONDARY WORKFLOWS - START AFTER 1B)
├─ 2A: Receptionist Dashboard & Appointment Modification
│  ├─ Requires: 1B.3, 2D.4
│  └─ Enables: 2B (Doctor requests), 3C (Surgery)
├─ 2B: Doctor Enhancements (Requests, Prescriptions, Todos)
│  ├─ Requires: 2D, 3B.2 (Nurse signatures)
│  └─ Enables: Complete doctor workflow
├─ 2C: Pharmacy Module (Stocks, Tracking)
│  ├─ Requires: 1B.4, 2B.2 (Prescriptions)
│  └─ Enables: Complete pharmacy workflow
└─ 2D: Appointment Request Workflow
   ├─ Requires: 1B.3 (Receptionist role)
   └─ Enables: 2A.6, 2B.1

PHASE 3 (SPECIALIZED MEDICAL WORKFLOWS - PARALLEL WITH PHASE 2)
├─ 3A: Nurse Module (Dashboard, To-Do, Signatures, Dialysis)
│  ├─ Requires: Nothing (can start in parallel)
│  └─ Enables: Complete nurse workflow
├─ 3B: Lab Agent (Dashboard, Report Upload)
│  ├─ Requires: ✅ Lab Request system
│  └─ Enables: Complete lab workflow
└─ 3C: Surgery Workflow (Planning, Reports, Surgeon Dashboard)
   ├─ Requires: 2A (Receptionist), ✅ Surgery Indication
   └─ Enables: Complete surgery workflow

PHASE 4 (COMMUNICATION - CAN START AFTER PHASE 1)
├─ 4A: Internal Staff Messaging
│  ├─ Requires: 1B (Role permissions)
│  └─ Enables: Day-to-day communication
└─ 4B: Notifications System
   ├─ Requires: 1B (Role permissions)
   └─ Enables: Application-wide alerts

PHASE 5 (GUARDIAN FEATURES - PARALLEL WITH PHASES 2-3)
├─ 5A: Guardian Document Upload
│  ├─ Requires: Nothing
│  └─ Enables: Document management
└─ 5B: Home Follow-Up Monitoring
   ├─ Requires: ✅ Discharge Follow-Up
   └─ Enables: Monitored home care

PHASE 6 (ADMIN FEATURES - START AFTER PHASE 2)
├─ 6A: Analytics & Statistics
│  ├─ Requires: 1B (Admin/HR roles)
│  └─ Enables: Management dashboards
├─ 6B: Audit Logs & Guidance
│  ├─ Requires: 1B
│  └─ Enables: Better usability
└─ 6C: Clinic Resources
   ├─ Requires: Nothing
   └─ Enables: Resource visualization

PHASE 7 (PERSONALIZATION - CAN START ANYTIME)
└─ 7A: User Settings & Profile
   ├─ Requires: 1A (Login)
   └─ Enables: Personalized experience

PHASE 8 (UI/UX - THROUGHOUT)
└─ 8A: Navigation & Layout
   ├─ Requires: Nothing (design work)
   └─ Enables: Professional interface
```

---

## Critical Path to MVP (Minimum Viable Product)

**Week 1-2: Foundation**
1. ✅ Phase 0 (already done)
2. ⏳ Phase 1A (Login, 2FA) - 20 hours
3. ⏳ Phase 1B (Role permissions) - 15 hours
4. ⏳ 8A (Navigation redesign) - 12 hours

**Week 3-4: Core Workflows**
1. ⏳ Phase 2A (Receptionist) - 25 hours
2. ⏳ Phase 2D (Appointment requests) - 15 hours
3. ⏳ Phase 2B (Doctor enhancements) - 20 hours
4. ⏳ Phase 3A (Nurse module) - 25 hours

**Week 5-6: Secondary Features**
1. ⏳ Phase 2C (Pharmacy) - 30 hours
2. ⏳ Phase 3C (Surgery) - 30 hours
3. ⏳ Phase 4B (Notifications) - 20 hours

**Week 7-8: Polish & Admin**
1. ⏳ Phase 4A (Staff messaging) - 25 hours
2. ⏳ Phase 6A (Analytics) - 25 hours
3. ⏳ Phase 5B (Home follow-up) - 25 hours

**Week 9-10: Refinement**
1. ⏳ Phase 6B (Audit/Help) - 20 hours
2. ⏳ Phase 7A (User settings) - 15 hours
3. ⏳ Phase 3B (Lab agent) - 10 hours
4. ⏳ Phase 5A (Guardian uploads) - 12 hours

---

## Prerequisite Summary Table

| Phase | Feature | Prerequisites | Blocked Until |
|-------|---------|----------------|---------------|
| 1A | Home CMS | None | Can start immediately |
| 1A | Remember Me | None | Can start immediately |
| 1A | Forgot Password | None | Can start immediately |
| 1A | 2FA | None | Can start immediately |
| 1B | HR Permissions | 1A | After login system |
| 1B | Admin Permissions | 1A | After login system |
| 1B | Receptionist Perms | 1A | After login system |
| 1B | Pharmacy Restriction | 1A | After login system |
| 2A | Receptionist Dashboard | 1B.3 | After role system |
| 2A | Appointment Modification | 1B.3 + 2D.4 | After requests system |
| 2A | Lab Visibility | 1B.3 | After role system |
| 2A | Room/Bed Assignment | 1B.3 | After role system |
| 2A | Appointment Requests Inbox | 1B.3 + 2D | After requests implemented |
| 2B | Doctor Requests | 2D | After requests system |
| 2B | Prescriptions | None | Can start in parallel |
| 2B | Hospitalization Todos | 3A.2 + 3A.3 | After nurse signatures |
| 2C | Pharmacy Module | 1B.4 + 2B.2 | After prescriptions |
| 2D | Appointment Requests | 1B.3 | After role system |
| 3A | Nurse Dashboard | None | Can start immediately |
| 3A | To-Do Lists | None | Can start immediately |
| 3A | Nurse Signatures | None | Can start immediately |
| 3B | Lab Agent Dashboard | ✅ Lab system | Can start immediately |
| 3C | Surgery Planning | 2A + ✅ Surgery Indication | After receptionist |
| 3C | Surgeon Dashboard | 2A + ✅ Surgery Indication | After receptionist |
| 4A | Staff Messaging | 1B | After role system |
| 4B | Notifications | 1B | After role system |
| 5A | Guardian Uploads | None | Can start immediately |
| 5B | Home Follow-Up | ✅ Discharge Follow-Up | Can start immediately |
| 6A | Analytics | 1B | After role system |
| 6B | Audit Logs | 1B | After role system |
| 7A | User Settings | 1A | After login system |
| 8A | Navigation Redesign | None | Can start immediately |

---

## Summary: What to Start With

### IMMEDIATE (Week 1 - No dependencies):
- ✅ All Phase 0 work (already done)
- ⏳ **1A.1** Home Page CMS
- ⏳ **1A.2** Remember Me
- ⏳ **1A.3** Forgot Password
- ⏳ **1A.4** Two-Factor Authentication
- ⏳ **8A.1** Left Sidebar Redesign
- ⏳ **8A.2** Responsive Layout Fix
- ⏳ **5A** Guardian Uploads (independent)

### AFTER LOGIN SYSTEM READY (Week 2-3):
- ⏳ **1B** Role-Based Permissions (all 4 features)
- ⏳ **7A** User Settings

### AFTER ROLE SYSTEM READY (Week 3+):
- ⏳ **2D** Appointment Request Workflow
- ⏳ **2A** Receptionist Module
- ⏳ **2B** Doctor Enhancements
- ⏳ **2C** Pharmacy Module
- ⏳ **4A** Staff Messaging
- ⏳ **4B** Notifications System
- ⏳ **6A** Analytics & Statistics
- ⏳ **6B** Audit Logs

### IN PARALLEL (anytime after Phase 0):
- ⏳ **3A** Nurse Module
- ⏳ **3B** Lab Agent
- ⏳ **3C** Surgery Workflow
- ⏳ **5B** Home Follow-Up
- ⏳ **6C** Clinic Resources

---

## Total Estimation

| Phase | Features | Est. Hours | Developers | Timeline |
|-------|----------|-----------|-----------|----------|
| Phase 1 | Auth + Roles | 35 hours | 1-2 | Week 1-2 |
| Phase 2 | Receptionist + Pharmacy + Doctor | 60 hours | 2-3 | Week 2-4 |
| Phase 3 | Nurse + Lab + Surgery | 65 hours | 2-3 | Week 3-5 |
| Phase 4 | Messaging + Notifications | 45 hours | 1-2 | Week 4-5 |
| Phase 5 | Guardian Features | 40 hours | 1 | Week 3-6 |
| Phase 6 | Admin & Analytics | 45 hours | 1-2 | Week 5-7 |
| Phase 7 | User Settings | 15 hours | 1 | Week 6-7 |
| Phase 8 | UI/UX Refinement | 20 hours | 1 | Throughout |
| **TOTAL** | **60+ Features** | **~325 hours** | **2-3 devs** | **8-10 weeks** |

**With 2 full-time developers: 16-20 weeks**  
**With 3 full-time developers: 10-13 weeks**  
**With 1 developer: 40-50 weeks** (not recommended)

