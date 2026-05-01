# IMPLEMENTATION NEXT STEPS - PHASES 10-12

**Date**: May 1, 2026 | **Priority**: 🔴 HIGH | **Timeline**: 10-15 hours

---

## 📋 PENDING WORK BREAKDOWN

### PHASE 10: AUTO-SCHEDULING (4-5 hours) 🔴 BLOCKING
**Objective**: Auto-create consultation after lab results processed  
**Dependency**: Phase 9 (lab integration) - ✅ COMPLETE

#### 10.1 Create ConsultationAutoSchedulingService
```
Endpoint: (Internal - no REST, event-driven only)
Trigger: LabResultProcessedEvent published from LabResultProcessingService
Logic:
  1. Receive LabResultProcessedEvent (includes patientId, consultationId)
  2. Verify patient record exists
  3. Create new Consultation record
  4. Set appointment_status = AWAITING_RECEPTIONIST_CONFIRMATION
  5. Set notes: "Auto-scheduled after lab results (CKD-EPI calculated)"
  6. Publish ConsultationAutoScheduledEvent for receptionist notification
```

#### 10.2 Add Event Listener
```
@EventListener
public void onLabResultProcessed(LabResultProcessedEvent event) {
  // Auto-create consultation
  // Publish notification event
}
```

#### 10.3 Database Update (Optional)
```sql
-- Add column to track auto-scheduled consultations
ALTER TABLE consultations 
ADD COLUMN auto_scheduled_from_lab_id UUID 
REFERENCES lab_result(id);
```

**Success Criteria**:
- [ ] Consultation auto-created within 1 second of lab upload
- [ ] Status set to AWAITING_RECEPTIONIST_CONFIRMATION
- [ ] Receptionist receives notification
- [ ] Event published successfully

---

### PHASE 11: RECEPTIONIST WORKFLOW (4-5 hours) 🔴 BLOCKING THEN AUTO-SCHEDULED CONSULTATIONS
**Objective**: Receptionist confirms and finalizes auto-scheduled appointments  
**Dependency**: Phase 10 complete

#### 11.1 Create ReceptionistAppointmentRequestService
```
Endpoints:
  GET    /api/appointments/pending-confirmation      → List auto-scheduled consultations
  POST   /api/appointments/{id}/confirm-details      → Receptionist enters date/time
  POST   /api/appointments/{id}/finalize             → Receptionist creates final appointment
  POST   /api/appointments/{id}/reject               → Reject auto-scheduling
```

#### 11.2 Service Logic
```java
public ReceptionistAppointmentRequestResponse confirmWithDetails(
    UUID consultationId,
    ReceptionistConfirmationRequest request  // contains appointment date/time, room, etc.
) {
  // 1. Load auto-scheduled consultation
  // 2. Validate date/time available (check room conflicts)
  // 3. Create Appointment record with confirmed details
  // 4. Update consultation status = APPOINTMENT_CONFIRMED
  // 5. Publish PatientAppointmentConfirmedEvent for notifications
  // 6. Return confirmed appointment details
}

public void rejectAutoScheduling(UUID consultationId, String reason) {
  // 1. Mark consultation as REJECTED_BY_RECEPTIONIST
  // 2. Publish notification to doctor: "Receptionist rejected auto-schedule"
  // 3. Provide reject reason to doctor
}
```

#### 11.3 Data Validation
```
Checks:
  - Room availability (no double-booking)
  - Doctor availability on that date
  - Patient has no conflicting appointments
  - Appointment window meets clinic hours
```

#### 11.4 Notification Flow
```
After Receptionist Confirms:
  1. Patient gets notification: "Your appointment confirmed for [date/time]"
  2. Doctor gets notification: "Appointment finalized by receptionist for [patient]"
  3. System logs audit trail: "Receptionist [name] confirmed appointment"
```

**Success Criteria**:
- [ ] Receptionist can view pending auto-scheduled consultations
- [ ] Can confirm with specific date/time
- [ ] Appointment created with confirmed details
- [ ] Patient/doctor receive notifications
- [ ] Can reject with reason
- [ ] Audit trail logged

---

### PHASE 12: DOCUMENTATION & COMPLIANCE (2 hours)
**Objective**: Complete medical/technical documentation  
**Dependency**: Phases 10-11 complete

#### 12.1 Swagger/OpenAPI Updates
```
Add to ConsultationMetricsResponse schema:
  - egfr: number (description: "Estimated Glomerular Filtration Rate (mL/min/1.73m²)")
  - ckdStage: string (enum: [G1, G2, G3A, G3B, G4, G5])
  - egfrQualityIndicator: string (enum: [HIGH, MEDIUM, LOW])
  - egfrTrend: string (enum: [STABLE, DECLINING, RAPID_DECLINE])
  - creatinineUmol: number (description: "Serum creatinine in SI units (µmol/L)")
  - egfrFormulaUsed: string (description: "Formula version (CKD_EPI_2021)")
  
Add note:
  "Calculated using CKD-EPI 2021 (European standard) with gender-specific coefficients.
   eGFR adjusted for age, sex, and serum creatinine. Quality indicator reflects data completeness
   and reliability of calculation."
```

#### 12.2 KDIGO Compliance Document
Create file: `docs/KDIGO_COMPLIANCE_REPORT.md`
```
Contents:
  1. Formula Reference (European CKD-EPI 2021)
     - Publication: KDIGO 2021 Clinical Practice Guideline
     - Link: https://kdigo.org/
  
  2. Stage Classification
     - Reference tables for all 5 stages
     - Associated clinical management recommendations
  
  3. Audit Trail
     - Every eGFR calculation logged
     - Includes: patient, date, values used, result, quality score
     - Retention: 7 years (medical record requirement)
  
  4. Quality Assurance
     - Formula verification: ±1.5 mL/min accuracy
     - Unit conversion validation: mg/dL ↔ µmol/L
     - Gender coefficient testing (1.018x factor)
```

#### 12.3 API Contract Documentation
Create file: `docs/CKD_EPI_API_CONTRACT.md`
```
Contents:
  - All 23 new ConsultationMetrics fields
  - Unit specifications (SI units, conversion factors)
  - Stage mapping (eGFR ranges → KDIGO stages)
  - Quality indicator definitions
  - Error codes and handling
  - Example requests/responses
```

#### 12.4 Data Model Diagram (Mermaid)
```
LabResult
  ├── Serum Creatinine
  ├── Test Date
  └── Quality Flag
        ↓ triggers
ConsultationMetrics
  ├── CKD-EPI Calculation
  ├── Stage Resolution
  ├── Trend Analysis
  └── Audit Trail
        ↓ triggers
ConsultationAutoScheduled
  └── Receptionist Confirmation
        ↓ creates
Appointment (CONFIRMED)
```

**Success Criteria**:
- [ ] Swagger docs complete and validated
- [ ] KDIGO compliance doc created
- [ ] Formula references cited
- [ ] Audit trail documented
- [ ] Data model diagrams created
- [ ] Version control documented

---

## 🎯 IMPLEMENTATION ROADMAP

```
Timeline (Cumulative Hours):
┌─────────────────────────────────────────────────────────────┐
│ Week 1:                                                     │
│ └─ Phase 10 (Auto-Scheduling) ............ 4-5 hours       │
│                                                             │
│ Week 2:                                                     │
│ ├─ Phase 11 (Receptionist Workflow) ...... 4-5 hours       │
│ └─ Phase 12 (Documentation) .............. 2 hours         │
│                                                             │
│ Week 3 (Optional - Enhancement):                           │
│ ├─ End-to-end testing ................... 3 hours          │
│ ├─ Performance optimization ............. 2 hours          │
│ └─ Security review ...................... 2 hours          │
└─────────────────────────────────────────────────────────────┘

Total Estimated: 15-20 hours (2-3 weeks for 1-2 developers)
```

---

## 🔧 TECH REQUIREMENTS

### Phase 10 Technologies
- Spring Events (already in use)
- Spring Data JPA (for new Consultation record)
- Event Publishing (ConsultationAutoScheduledEvent)

### Phase 11 Technologies
- Spring MVC (REST endpoints)
- Validation framework (room availability, conflicts)
- Transaction management (atomic confirmation)
- Notification service (patient/doctor alerts)

### Phase 12 Technologies
- Swagger/OpenAPI (documentation)
- Markdown (documentation files)
- Mermaid (diagrams)

---

## ✅ TESTING CHECKLIST

### Phase 10 Tests
```
Test Case: Lab Upload → Consultation Auto-Creation
  1. Upload lab result for patient
  2. Verify LabResultProcessedEvent published
  3. Verify ConsultationAutoSchedulingService receives event
  4. Verify Consultation record created
  5. Verify status = AWAITING_RECEPTIONIST_CONFIRMATION
  6. Verify Receptionist gets notification
```

### Phase 11 Tests
```
Test Case: Receptionist Confirms Appointment
  1. Receptionist views pending list
  2. Receptionist selects consultation
  3. Receptionist enters date/time
  4. Verify room availability check
  5. Verify Appointment record created
  6. Verify Patient notification sent
  7. Verify Doctor notification sent
  
Test Case: Receptionist Rejects Appointment
  1. Receptionist rejects auto-schedule
  2. Verify status = REJECTED_BY_RECEPTIONIST
  3. Verify Doctor notified with reason
```

### Phase 12 Tests
```
Test Case: Documentation Completeness
  1. Swagger docs accessible
  2. All fields documented with descriptions
  3. KDIGO compliance doc complete
  4. Formula references present
  5. Audit trail samples included
```

---

## 📊 SUCCESS METRICS

| Phase | Metric | Target |
|-------|--------|--------|
| 10 | Consultation auto-created within | 1 second of lab upload |
| 10 | Event publication success rate | 100% |
| 11 | Room conflict prevention | 100% |
| 11 | Notification delivery rate | 99%+ |
| 11 | Receptionist processing time | <2 minutes per confirmation |
| 12 | Documentation completeness | 100% of new fields documented |
| 12 | KDIGO compliance | ✅ Verified |

---

## 🚀 DEPLOYMENT PREREQUISITES

Before deploying phases 10-12, ensure:
- [x] All phases 1-9 complete and tested
- [x] Lab integration end-to-end verified
- [ ] Receptionist role permissions configured
- [ ] Notification service deployed and tested
- [ ] Database backups configured
- [ ] Monitoring/alerting setup for new services
