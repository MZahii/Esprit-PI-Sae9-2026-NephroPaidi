# Remaining Requirements - Implementation Plan

## Overview
**7 requirements remaining (26% of total)**  
**Estimated effort: 15-20 hours** (can be done in parallel tracks)

---

## TRACK 1: Appointment Request Workflow (High Priority)

### Requirement D: Doctor Should NOT Directly Schedule Appointments

**Current State**: ❌ Not enforced  
**Desired State**: Doctor sends request → Receptionist approves → Appointment created

**What's Needed**:

### 1.1 Backend: Create AppointmentRequest Entity
**Necessity**: Core data model for workflow  
**File**: `BackEnd/microservices/clinical-service/src/main/java/tn/esprit/spring/clinicalservice/appointmentRequest/entity/AppointmentRequest.java`

```java
@Entity
@Table(name = "appointment_requests")
public class AppointmentRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "doctor_id") private UUID doctorId;
    @Column(name = "patient_id") private Long patientId;
    @Column(name = "requested_date") private LocalDateTime requestedDate;
    @Column(name = "reason") private String reason;
    @Column(name = "urgency") private String urgency; // ROUTINE, URGENT
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status") private AppointmentRequestStatus status; // PENDING, APPROVED, REJECTED, SCHEDULED
    
    @Column(name = "receptionist_notes") private String receptionistNotes;
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}

public enum AppointmentRequestStatus {
    PENDING,      // Waiting for receptionist
    APPROVED,     // Receptionist approved, waiting for scheduling
    SCHEDULED,    // Appointment created
    REJECTED,     // Receptionist rejected
    CANCELLED     // Doctor or system cancelled
}
```

**Dependencies**: None (new entity)  
**Est. Effort**: 30 minutes

---

### 1.2 Database: Flyway Migration V12
**File**: `BackEnd/microservices/clinical-service/src/main/resources/db/migration/V12__create_appointment_requests_table.sql`

```sql
CREATE TABLE IF NOT EXISTS appointment_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    patient_id BIGINT NOT NULL,
    requested_date TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    urgency VARCHAR(20) DEFAULT 'ROUTINE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    receptionist_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_req_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_appt_req_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE INDEX idx_appt_req_doctor_id ON appointment_requests(doctor_id);
CREATE INDEX idx_appt_req_patient_id ON appointment_requests(patient_id);
CREATE INDEX idx_appt_req_status ON appointment_requests(status);
CREATE INDEX idx_appt_req_requested_date ON appointment_requests(requested_date);
```

**Dependencies**: None  
**Est. Effort**: 15 minutes

---

### 1.3 Backend: Repository + Service + Controller
**Files**:
- `AppointmentRequestRepository` (interface with custom queries)
- `AppointmentRequestService` + `AppointmentRequestServiceImpl`
- `AppointmentRequestController` (3 endpoints: create, list pending, approve/reject)

**Endpoints**:
```
POST   /api/clinical/appointment-requests           → Doctor sends request
GET    /api/clinical/appointment-requests/my        → Doctor views own requests
GET    /api/clinical/appointment-requests/pending   → Receptionist inbox (role-based)
PATCH  /api/clinical/appointment-requests/{id}/approve  → Receptionist approves
PATCH  /api/clinical/appointment-requests/{id}/reject   → Receptionist rejects
```

**Key Business Logic**:
- Doctor cannot schedule directly; must send request first
- Receptionist receives pending requests
- Once approved, receptionist creates actual Appointment
- Priority flag: Doctor-requested appointments marked as HIGH priority

**Dependencies**: 
- AppointmentRequest entity (1.1)
- Flyway migration (1.2)

**Est. Effort**: 2-3 hours

---

### 1.4 Frontend: Appointment Request Form
**File**: `FrontEnd/src/app/pages/backoffice/appointments/appointment-request.component.ts`

**Features**:
- Form to request appointment (patient dropdown, date picker, reason, urgency)
- Shows confirmation message
- Lists user's pending requests with statuses

**Replaces**: Current direct appointment creation

**Dependencies**: 
- Backend endpoints (1.3)
- API service update

**Est. Effort**: 1.5-2 hours

---

### 1.5 Frontend: Receptionist Inbox (New Component)
**File**: `FrontEnd/src/app/pages/backoffice/receptionist/appointment-requests-inbox.component.ts`

**Features**:
- List of pending appointment requests
- Approve/Reject buttons with reason field
- Filter by urgency/date
- Once approved, shows form to create actual appointment

**Dependencies**: 
- Backend endpoints (1.3)
- Role-based access control

**Est. Effort**: 2-2.5 hours

---

**TRACK 1 TOTAL: ~10-12 hours**

---

## TRACK 2: Prescription Flow (Medium Priority)

### Requirement F: Prescription Flow

**Current State**: ❌ Completely missing  
**Desired State**: Doctor creates prescription → Pharmacy receives → Pharmacist processes

**What's Needed**:

### 2.1 Backend: Create Prescription Entity
**File**: `BackEnd/microservices/clinical-service/src/main/java/tn/esprit/spring/clinicalservice/prescription/entity/Prescription.java`

```java
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "doctor_id") private UUID doctorId;
    @Column(name = "patient_id") private Long patientId;
    @Column(name = "notes") private String clinicalNotes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status") private PrescriptionStatus status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrescriptionItem> items = new ArrayList<>();
    
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    
    @Column(name = "drug_name") private String drugName;
    @Column(name = "dosage") private String dosage; // e.g., "500mg"
    @Column(name = "frequency") private String frequency; // e.g., "2x daily"
    @Column(name = "duration_days") private Integer durationDays;
    @Column(name = "instructions") private String instructions;
    
    @CreationTimestamp @Column(name = "created_at") private LocalDateTime createdAt;
}

public enum PrescriptionStatus {
    PENDING,      // Waiting for pharmacy
    IN_PROGRESS,  // Pharmacist processing
    COMPLETED,    // Filled/prepared
    CANCELLED
}
```

**Dependencies**: None (new entity)  
**Est. Effort**: 45 minutes

---

### 2.2 Database: Flyway Migrations V13-V14
**Files**:
- `V13__create_prescriptions_table.sql`
- `V14__create_prescription_items_table.sql`

**Dependencies**: None  
**Est. Effort**: 20 minutes

---

### 2.3 Backend: Service + Controller
**Files**:
- `PrescriptionRepository`
- `PrescriptionService` + `PrescriptionServiceImpl`
- `PrescriptionController` (endpoints: create, list by doctor/patient, get single)

**Endpoints**:
```
POST   /api/clinical/prescriptions              → Doctor creates
GET    /api/clinical/prescriptions/my           → Doctor views own
GET    /api/clinical/prescriptions/patient/{id} → View patient's prescriptions
GET    /api/clinical/prescriptions/{id}         → Get single
PATCH  /api/clinical/prescriptions/{id}/status → Update status
```

**Dependencies**: 
- Prescription entities (2.1)
- Flyway migrations (2.2)

**Est. Effort**: 2-2.5 hours

---

### 2.4 Pharmacy Service Integration (Optional)
**Goal**: Notify pharmacy-service of new prescriptions

**Implementation**:
- HTTP POST to `pharmacy-service` when prescription created
- Or: Publish event to message queue (RabbitMQ if available)
- Pharmacy-service subscribes and creates inbox entry

**Dependencies**: 
- pharmacy-service exists and has endpoint
- Messaging infrastructure (RabbitMQ/Kafka) if event-based

**Est. Effort**: 1-2 hours (if needed)

---

### 2.5 Frontend: Prescription Form
**File**: `FrontEnd/src/app/pages/backoffice/prescriptions/prescription-form.component.ts`

**Features**:
- Patient selector
- FormArray for multiple drugs
- Each item: drug name, dosage, frequency, duration, instructions
- Submit → creates prescription

**Dependencies**: 
- Backend endpoints (2.3)

**Est. Effort**: 1.5-2 hours

---

### 2.6 Frontend: Pharmacy Inbox (New Component)
**File**: `FrontEnd/src/app/pages/pharmacy/prescription-inbox.component.ts`

**Features**: (For pharmacy role)
- List pending prescriptions
- Mark as in-progress / completed
- Notes field
- Patient info view

**Dependencies**: 
- Backend endpoints (2.3)
- Role-based access (Pharmacy role)

**Est. Effort**: 1.5-2 hours

---

**TRACK 2 TOTAL: ~10-13 hours**

---

## TRACK 3: UI/UX Improvements (Lower Priority)

### Requirement C: Today Appointments Page Enhancement

**Current State**: ⚠️ 50% done (start button exists, UI could be better)  
**Desired State**: Better visibility, status indication, time countdown

**What's Needed**:

### 3.1 Frontend: Enhanced Appointments Component
**File**: Update `FrontEnd/src/app/pages/backoffice/appointments/appointments.component.ts`

**Improvements**:
- Group appointments by time (Morning, Afternoon, Evening)
- Show countdown timer to 15-min window start
- Status badges (Scheduled, Confirmed, Cancelled, No-show)
- Patient name + patient ID display
- Color coding by urgency (if available in data)

**Dependencies**: 
- Existing Appointment data model

**Est. Effort**: 1-1.5 hours

---

### 3.2 Frontend: Add Appointment Filtering
**Features**:
- Filter by status (All, Scheduled, Confirmed, Cancelled)
- Filter by time (Today only, or expand to this week)
- Search by patient name

**Dependencies**: 
- Enhanced component (3.1)

**Est. Effort**: 1 hour

---

**TRACK 3 TOTAL: ~2-2.5 hours**

---

## TRACK 4: Role-Based Visibility (Lower Priority)

### Requirement E (partial): Receptionist Lab Visibility

**Current State**: ❌ Not filtered by role  
**Desired State**: Receptionist sees minimal lab details; full details for doctors

**What's Needed**:

### 4.1 Backend: Role-Based Filtering in Service
**Changes**:
- In `LabRequestService`, add role check
- If RECEPTIONIST role: return only `{id, patientId, testType, status, notes}`
- If DOCTOR role: return full details including urgency, doctor notes, results

**Dependencies**: 
- Existing LabRequest service
- Authentication with roles

**Est. Effort**: 45 minutes

---

### 4.2 Frontend: Conditional Field Display
**Changes**: 
- Lab inbox component checks user role
- Show/hide fields based on role

**Dependencies**: 
- AuthService with role info
- Backend role-based filtering (4.1)

**Est. Effort**: 30 minutes

---

**TRACK 4 TOTAL: ~1.25 hours**

---

## TRACK 5: Advanced Features (Optional/Low Priority)

### Doctor Hospitalization Management
**Components Needed**:
- Hospitalized patients list view
- Discharge summary form
- Hospital ward/bed assignment visibility

**Est. Effort**: 4-5 hours (low priority, can defer)

---

## Master Timeline

| Track | Feature | Effort | Priority | Notes |
|-------|---------|--------|----------|-------|
| 1 | Appointment Requests | 10-12h | **CRITICAL** | Blocks workflow, enforces proper process |
| 2 | Prescriptions | 10-13h | **HIGH** | New feature, integrates pharmacy |
| 3 | UI Improvements | 2-2.5h | **MEDIUM** | Polish, doesn't block core workflow |
| 4 | Role-Based Filtering | 1.25h | **LOW** | Nice-to-have, easy to add |
| 5 | Hospitalization | 4-5h | **LOW** | Can defer to next sprint |

**Total**: ~28-34 hours (can be parallelized, 1-2 developers, ~4-5 days if working full-time)

---

## Execution Strategy

### Phase 1 (Days 1-2): Critical Path
1. ✅ Appointment Requests (Track 1) - enforces proper workflow
2. ⏳ Prescriptions (Track 2) - new major feature

### Phase 2 (Day 3): Polish & Integration
3. ⏳ UI Improvements (Track 3) - better user experience
4. ⏳ Role-Based Filtering (Track 4) - security & visibility

### Phase 3 (Day 4+): Optional
5. ⏳ Hospitalization Management (Track 5) - future enhancement

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Pharmacy service not ready | Medium | High | Can implement pharmacy-service endpoints in parallel |
| Role/auth system incomplete | Low | Medium | Use existing Keycloak setup |
| Complex form validations | Low | Low | Use Angular built-in validators |
| Database migration conflicts | Low | High | Test migrations in dev first, use Flyway correctly |
| API naming conflicts | Very Low | Low | Follow existing patterns strictly |

---

## Deliverables Checklist

### Track 1: Appointment Requests
- [ ] AppointmentRequest entity
- [ ] Flyway migration V12
- [ ] Repository, Service, Controller
- [ ] Frontend form component
- [ ] Receptionist inbox component
- [ ] Integration test

### Track 2: Prescriptions
- [ ] Prescription + PrescriptionItem entities
- [ ] Flyway migrations V13-V14
- [ ] Repository, Service, Controller
- [ ] Frontend prescription form
- [ ] Pharmacy inbox component
- [ ] (Optional) Pharmacy service integration

### Track 3: UI/UX
- [ ] Enhanced appointments component
- [ ] Filtering logic
- [ ] SCSS improvements

### Track 4: Role-Based
- [ ] Service-level filtering
- [ ] Frontend conditional display

### Track 5: Hospitalization
- [ ] (Defer to next sprint)

---

## Dependencies Map

```
┌─────────────────────────────────────────┐
│   CRITICAL PATH (Appointment Requests)  │
│  (10-12h, unblocks receptionist work)   │
└─────────────────────────────────────────┘
                    ↓
            ┌───────────────┐
            │ Prescription  │ (10-13h)
            │ Flow (Track 2)│
            └───────────────┘
                    ↓
        ┌─────────────────────┐
        │ UI & Filtering      │ (3-4h)
        │ (Tracks 3 & 4)      │
        └─────────────────────┘
```

**No blocking dependencies between Track 1 & 2** — can start both simultaneously with different team members.

---

## Code Quality Standards

- Follow `.instructions.md` patterns strictly
- Compile: `mvn clean compile` must pass without errors
- Frontend: `npm run build` must complete
- Database: Run Flyway migrations in dev first
- Commit message format: `feature: Add appointment request workflow`
- Update REQUIREMENT_ANALYSIS.md as features complete

---

## Success Criteria

When complete:
1. ✅ All 27 requirements implemented (100% coverage)
2. ✅ Backend compiles with `mvn package -DskipTests` (no errors)
3. ✅ Frontend builds with `npm run build` (no errors, warnings OK)
4. ✅ All Flyway migrations run successfully
5. ✅ Manual API testing via Swagger passes
6. ✅ REQUIREMENT_ANALYSIS.md shows 100% completion
