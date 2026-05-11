# STUDENT C: Procedure-Service + Surgery Operations (FIFO + Scheduling)

**Duration**: 3 Days × 7 hours/day = 21 hours  
**Service**: `BackEnd/microservices/procedure-service`  
**Objective**: Implement surgical referral FIFO queue with atomic pessimistic locking, surgeon availability calculation, pre-op readiness model, and receptionist scheduling workflow.

---

## DAY 1: FIFO Surgery Queue + Atomic Locking (7 hours)

### 1.1 Entity: SurgeryReferralRequest (FIFO Core) (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/entity/`

**SurgeryReferralRequest.java**:
```java
@Entity
@Table(name = "surgery_referral_requests", indexes = {
    @Index(name = "idx_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_surgeon_status", columnList = "surgeonId, status")
})
public class SurgeryReferralRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcedureType procedureType;  // NEPHROSTOMY, RENAL_BIOPSY, HEMODIALYSIS_FISTULA, TRANSPLANT_EVALUATION, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status;  // PENDING, OFFERED, ACCEPTED_BY_SURGEON, SCHEDULED, COMPLETED, REJECTED, CANCELLED
    
    @Column(nullable = false)
    private UUID referringPhysicianId;  // who requested the surgery
    
    @Column(nullable = false)
    private LocalDateTime createdAt;  // FIFO: sort by this
    
    @Column(columnDefinition = "TEXT")
    private String clinicalIndication;
    
    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;  // ELECTIVE, URGENT, EMERGENCY
    
    // FIFO mechanics: pessimistic lock field
    @Version  // Optimistic lock (alternative: use @Lock(LockModeType.PESSIMISTIC_WRITE))
    private Integer version;
    
    // Offer tracking
    private UUID surgeonOfferedId;
    private LocalDateTime surgeonOfferredAt;
    private LocalDateTime surgeonOfferExpiresAt;  // offer valid for 24h
    
    // Acceptance tracking
    private UUID acceptingSurgeonId;  // who accepted the offer
    private LocalDateTime acceptedAt;
    
    // Scheduling
    private UUID scheduledOperationId;  // FK to OperationExecution
    private LocalDateTime scheduledDateTime;
    
    // Completion
    private LocalDateTime completedAt;
    
    // getters/setters
}

public enum ReferralStatus {
    PENDING,              // Created, waiting for surgeon offer
    OFFERED,              // Surgeon offered, waiting for acceptance
    ACCEPTED_BY_SURGEON,  // Surgeon accepted, ready for scheduling
    SCHEDULED,            // Receptionist scheduled operation
    COMPLETED,            // Operation completed
    REJECTED,             // Surgeon rejected
    CANCELLED             // Referrer or patient cancelled
}

public enum ProcedureType {
    NEPHROSTOMY,
    RENAL_BIOPSY,
    HEMODIALYSIS_FISTULA,
    TRANSPLANT_EVALUATION,
    UROLOGICAL_PROCEDURE,
    SPINAL_PROCEDURE,
    EMERGENCY_SURGERY
}

public enum UrgencyLevel {
    ELECTIVE,   // Can wait weeks
    URGENT,     // Should be done in 1-2 weeks
    EMERGENCY   // Must be done today/tomorrow
}
```

---

### 1.2 FIFOSurgeryQueueService: Atomic First-Wins Logic (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/service/FIFOSurgeryQueueService.java`

**Critical: Pessimistic lock on row selection**:
```java
@Service
@Transactional
public class FIFOSurgeryQueueService {
    
    @Autowired private SurgeryReferralRequestRepository referralRepository;
    @Autowired private SurgeonAvailabilityService availabilityService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    /**
     * Get next eligible referral from FIFO queue for a surgeon.
     * ATOMIC: Pessimistic lock prevents race condition (only 1 surgeon can claim each referral).
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)  // CRITICAL: strictest isolation
    public Optional<SurgeryReferralRequest> claimNextReferral(UUID surgeonId) {
        // Pessimistically lock rows to prevent concurrent claims
        // SELECT * FROM surgery_referral_requests 
        // WHERE status = 'PENDING' 
        // ORDER BY createdAt ASC, urgency DESC
        // LIMIT 1
        // FOR UPDATE NOWAIT;
        
        List<SurgeryReferralRequest> fifoQueue = referralRepository
            .findByStatusOrderByCreatedAtAscUrgencyDesc(ReferralStatus.PENDING);
        
        if (fifoQueue.isEmpty()) {
            return Optional.empty();
        }
        
        SurgeryReferralRequest referral = fifoQueue.get(0);  // FIFO: earliest created
        
        // Check surgeon availability before claiming
        SurgeonAvailabilityReport availability = availabilityService.getAvailability(surgeonId);
        if (!availability.isAvailable()) {
            return Optional.empty();  // Surgeon not available; next surgeon can try
        }
        
        // ATOMIC: Update to OFFERED; if another surgeon claimed it concurrently, 
        // OptimisticLockException thrown → retry logic at endpoint
        referral.setSurgeonOfferedId(surgeonId);
        referral.setSurgeonOfferredAt(LocalDateTime.now());
        referral.setSurgeonOfferExpiresAt(LocalDateTime.now().plusDays(1));  // 24h to accept
        referral.setStatus(ReferralStatus.OFFERED);
        referral = referralRepository.save(referral);
        
        eventPublisher.publishEvent(new SurgeryOfferMadeEvent(this, referral.getId(), 
                                   surgeonId, referral.getPatientId()));
        
        return Optional.of(referral);
    }
    
    /**
     * Surgeon accepts the offer → move to ACCEPTED_BY_SURGEON.
     * Validate: only offering surgeon can accept, offer not expired.
     */
    public SurgeryReferralRequest acceptOffer(UUID referralId, UUID surgeonId) {
        SurgeryReferralRequest referral = referralRepository.findById(referralId).orElseThrow();
        
        if (referral.getStatus() != ReferralStatus.OFFERED) {
            throw new InvalidStateException("Referral not in OFFERED state");
        }
        
        if (!referral.getSurgeonOfferedId().equals(surgeonId)) {
            throw new ForbiddenException("Only offering surgeon can accept");
        }
        
        if (LocalDateTime.now().isAfter(referral.getSurgeonOfferExpiresAt())) {
            throw new OfferExpiredException("Surgeon offer expired at " + referral.getSurgeonOfferExpiresAt());
        }
        
        referral.setAcceptingSurgeonId(surgeonId);
        referral.setAcceptedAt(LocalDateTime.now());
        referral.setStatus(ReferralStatus.ACCEPTED_BY_SURGEON);
        referral = referralRepository.save(referral);
        
        eventPublisher.publishEvent(new SurgeryAcceptedEvent(this, referral.getId(), 
                                   surgeonId, referral.getPatientId()));
        
        return referral;
    }
    
    /**
     * Surgeon rejects the offer → back to PENDING for other surgeons to claim.
     */
    public SurgeryReferralRequest rejectOffer(UUID referralId, UUID surgeonId, String reason) {
        SurgeryReferralRequest referral = referralRepository.findById(referralId).orElseThrow();
        
        if (referral.getStatus() != ReferralStatus.OFFERED) {
            throw new InvalidStateException("Referral not in OFFERED state");
        }
        
        if (!referral.getSurgeonOfferedId().equals(surgeonId)) {
            throw new ForbiddenException("Only offering surgeon can reject");
        }
        
        referral.setSurgeonOfferedId(null);
        referral.setSurgeonOfferredAt(null);
        referral.setSurgeonOfferExpiresAt(null);
        referral.setStatus(ReferralStatus.PENDING);  // Back to PENDING
        referral = referralRepository.save(referral);
        
        eventPublisher.publishEvent(new SurgeryRejectedEvent(this, referral.getId(), 
                                   surgeonId, reason));
        
        return referral;
    }
}
```

**CRITICAL: Why pessimistic lock matters**:
```
Without lock:
  Surgeon A queries: SELECT * FROM referrals WHERE status='PENDING' LIMIT 1 → Ref #123
  Surgeon B queries: SELECT * FROM referrals WHERE status='PENDING' LIMIT 1 → Ref #123 (same!)
  Both update Ref #123 to OFFERED → Race condition! B overwrites A's offer.

With FOR UPDATE NOWAIT:
  Surgeon A queries: SELECT ... WHERE status='PENDING' LIMIT 1 FOR UPDATE NOWAIT → Ref #123 (locked)
  Surgeon B queries: SELECT ... WHERE status='PENDING' LIMIT 1 FOR UPDATE NOWAIT → LOCKED (waits or fails)
  Only A can claim Ref #123.
```

---

### 1.3 SurgeonAvailabilityService (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/service/SurgeonAvailabilityService.java`

**Implement**:
```java
@Service
public class SurgeonAvailabilityService {
    
    @Autowired private OperationExecutionRepository operationRepository;
    @Autowired private SurgeonScheduleRepository scheduleRepository;
    
    /**
     * Calculate surgeon availability based on:
     * 1. Operating room schedule (booked time slots)
     * 2. Surgeon's own committed slots (operations in progress, post-op rounds)
     * 3. Surgeon's maximum operations per week (e.g., 10 operations/week)
     */
    public SurgeonAvailabilityReport getAvailability(UUID surgeonId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusWeeks(1);
        
        // Count operations scheduled in next 7 days
        List<OperationExecution> upcomingOps = operationRepository
            .findBySurgeonIdAndScheduledDateTimeBetweenAndStatusNotIn(
                surgeonId, now, nextWeek, 
                List.of(OperationStatus.CANCELLED, OperationStatus.ABORTED)
            );
        
        int operationsThisWeek = upcomingOps.size();
        int maxOpsPerWeek = 10;  // Policy constant
        
        // Get surgeon's preferred operating room schedule
        SurgeonSchedule schedule = scheduleRepository.findBySurgeonId(surgeonId)
            .orElse(createDefaultSchedule(surgeonId));
        
        // Find next available slot (day + time)
        LocalDateTime nextAvailableSlot = findNextAvailableSlot(surgeonId, now, schedule);
        
        boolean isAvailable = operationsThisWeek < maxOpsPerWeek && nextAvailableSlot != null;
        
        return new SurgeonAvailabilityReport(
            surgeonId,
            isAvailable,
            operationsThisWeek,
            maxOpsPerWeek,
            nextAvailableSlot,
            upcomingOps.size()
        );
    }
    
    /**
     * Find next available operating room slot for surgeon.
     * Consider: OR availability, surgeon availability, patient preparation time.
     */
    private LocalDateTime findNextAvailableSlot(UUID surgeonId, LocalDateTime from,
                                               SurgeonSchedule schedule) {
        // Iterate through next 30 days
        for (int day = 0; day < 30; day++) {
            LocalDateTime checkDate = from.plusDays(day).withHour(8).withMinute(0);
            int dayOfWeek = checkDate.getDayOfWeek().getValue();  // 1=Monday, 7=Sunday
            
            // Check if surgeon works this day
            if (!schedule.getWorkingDays().contains(dayOfWeek)) {
                continue;  // Surgeon doesn't work this day
            }
            
            // Check OR availability for each time slot (e.g., 08:00, 09:00, ..., 17:00)
            for (int hour = 8; hour <= 16; hour++) {  // 8am to 4pm
                LocalDateTime slotTime = checkDate.withHour(hour);
                
                if (isSlotAvailable(surgeonId, slotTime, schedule)) {
                    return slotTime;
                }
            }
        }
        
        return null;  // No availability found in 30 days
    }
    
    private boolean isSlotAvailable(UUID surgeonId, LocalDateTime slotTime,
                                   SurgeonSchedule schedule) {
        // Check if OR is already booked at this time
        boolean orBooked = operationRepository.existsByScheduledDateTimeAndStatus(
            slotTime, OperationStatus.SCHEDULED
        );
        
        if (orBooked) return false;
        
        // Check if surgeon is already committed at this time
        boolean surgeonBusy = operationRepository
            .existsBySurgeonIdAndScheduledDateTimeBetween(
                surgeonId,
                slotTime.minusHours(1),  // Account for pre-op + operation time
                slotTime.plusHours(3)    // Typical operation duration
            );
        
        return !surgeonBusy;
    }
}

public record SurgeonAvailabilityReport(
    UUID surgeonId,
    boolean available,
    int operationsThisWeek,
    int maxOperationsPerWeek,
    LocalDateTime nextAvailableSlot,
    int upcomingOperations
) {}
```

---

### 1.4 SurgeonSchedule Entity (1 hour)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/entity/SurgeonSchedule.java`

**Implement**:
```java
@Entity
@Table(name = "surgeon_schedules")
public class SurgeonSchedule {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private UUID surgeonId;
    
    @ElementCollection
    @CollectionTable(name = "surgeon_working_days")
    @Column(name = "day_of_week")  // 1=Monday, 7=Sunday
    private Set<Integer> workingDays;  // e.g., {1,2,3,4,5} for Mon-Fri
    
    @Column(nullable = false)
    private Integer startHour;  // 8 = 08:00
    
    @Column(nullable = false)
    private Integer endHour;    // 17 = 17:00
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatingRoomType preferredOR;  // OR1, OR2, OR3, CARDIAC, TRAUMA
    
    private Integer maxOperationsPerWeek;  // null = use service default (10)
    
    // getters/setters
}

public enum OperatingRoomType {
    OR1, OR2, OR3, OR4, OR5, CARDIAC, TRAUMA, ENDOSCOPY
}
```

---

## DAY 2: Pre-Op Readiness + Scheduling Workflow (7 hours)

### 2.1 PreOpReadiness Entity + Validator (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/entity/PreOpReadiness.java`

**Implement**:
```java
@Entity
@Table(name = "preop_readiness")
public class PreOpReadiness {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private UUID referralId;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreOpReadinessStatus status;  // PENDING_ASSESSMENT, IN_PROGRESS, READY, HOLD, CONTRAINDICATED
    
    // Component checks (each must pass before operation)
    private Boolean hasConsultationApproval;      // Referring physician sign-off
    private Boolean hasPreOpLabResults;           // Recent creatinine, K+, Ca2+, Hb
    private Boolean hasNephropathyAssessment;     // eGFR, CKD stage, renal function
    private Boolean hasAnesthesiaConsent;         // Anesthesiologist clearance
    private Boolean hasMedicationReview;          // Pharmacist reviewed pre-op/post-op meds
    private Boolean hasAllergyVerification;       // Drug allergies confirmed
    private Boolean hasPhysicalExamClearance;     // General physical exam passed
    private Boolean hasCoagulationProfile;        // PT/INR, aPTT normal
    private Boolean hasCardiacClearance;          // ECG, echo if needed
    private Boolean hasInfectionScreening;        // No active infection
    private Boolean hasNutritionAssessment;       // Adequate nutrition for healing
    
    // Holds/alerts
    @Enumerated(EnumType.STRING)
    @ElementCollection
    private Set<PreOpHoldReason> holds;  // {HIGH_POTASSIUM, PENDING_LAB, INFECTION_DETECTED, ...}
    
    @Column(columnDefinition = "TEXT")
    private String readinessNotes;
    
    @Column(nullable = false)
    private UUID assessingPhysicianId;
    
    @Column(nullable = false)
    private LocalDateTime assessmentDate;
    
    // getters/setters
}

public enum PreOpReadinessStatus {
    PENDING_ASSESSMENT,  // Referral accepted, assessment not started
    IN_PROGRESS,         // Checks running
    READY,               // All checks passed, cleared for surgery
    HOLD,                // One or more checks failed; must resolve before surgery
    CONTRAINDICATED      // Patient unsuitable for surgery (e.g., terminal illness)
}

public enum PreOpHoldReason {
    HIGH_POTASSIUM,
    HIGH_CREATININE,
    PENDING_LAB,
    INFECTION_DETECTED,
    BLEEDING_DISORDER,
    CARDIAC_ISSUE,
    ANESTHESIA_RISK,
    MEDICATION_CONFLICT,
    CONSENT_PENDING,
    OTHER
}
```

---

### 2.2 PreOpReadinessService + Validator (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/service/PreOpReadinessService.java`

**Implement**:
```java
@Service
@Transactional
public class PreOpReadinessService {
    
    @Autowired private PreOpReadinessRepository readinessRepository;
    @Autowired private SurgeryReferralRequestRepository referralRepository;
    @Autowired private RestTemplate restTemplate;  // Call clinical-service via HTTP
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    /**
     * Create pre-op readiness assessment when referral accepted.
     */
    public PreOpReadiness createAssessment(UUID referralId, UUID assessingPhysicianId) {
        SurgeryReferralRequest referral = referralRepository.findById(referralId).orElseThrow();
        
        PreOpReadiness readiness = new PreOpReadiness();
        readiness.setReferralId(referralId);
        readiness.setPatientId(referral.getPatientId());
        readiness.setStatus(PreOpReadinessStatus.PENDING_ASSESSMENT);
        readiness.setAssessingPhysicianId(assessingPhysicianId);
        readiness.setAssessmentDate(LocalDateTime.now());
        
        return readinessRepository.save(readiness);
    }
    
    /**
     * Publish event to trigger assessment checks across services.
     * clinical-service: fetch labs, eGFR, nephropathy data
     * pharmacy-service: review pre-op medications
     * This is an asynchronous process; result comes back via event.
     */
    public void startAssessment(UUID readinessId) {
        PreOpReadiness readiness = readinessRepository.findById(readinessId).orElseThrow();
        
        readiness.setStatus(PreOpReadinessStatus.IN_PROGRESS);
        readinessRepository.save(readiness);
        
        // Publish event that triggers cross-service checks
        eventPublisher.publishEvent(new PreOpReadinessRequestedEvent(this, 
                                   readiness.getReferralId(), 
                                   readiness.getPatientId()));
    }
    
    /**
     * Receive check results from clinical-service (labs, nephropathy).
     */
    @EventListener
    public void onPreOpLabsReceived(PreOpLabsReadyEvent event) {
        PreOpReadiness readiness = readinessRepository.findByReferralId(event.getReferralId()).orElseThrow();
        
        // Validate thresholds
        readiness.setHasPreOpLabResults(true);
        readiness.setHasNephropathyAssessment(true);
        
        // Check for dangerous values
        Set<PreOpHoldReason> newHolds = new HashSet<>();
        if (event.getSerumPotassium_mmolL().compareTo(new BigDecimal("6.0")) > 0) {
            newHolds.add(PreOpHoldReason.HIGH_POTASSIUM);
        }
        if (event.getSerumCreatinine_umolL().compareTo(new BigDecimal("300")) > 0) {
            newHolds.add(PreOpHoldReason.HIGH_CREATININE);
        }
        
        if (!newHolds.isEmpty()) {
            readiness.setHolds(newHolds);
            readiness.setStatus(PreOpReadinessStatus.HOLD);
            
            // Publish alert event
            eventPublisher.publishEvent(new PreOpHoldAlertEvent(this, 
                                       readiness.getReferralId(), newHolds));
        } else {
            // Remove holds if previously set
            readiness.getHolds().removeAll(newHolds);
        }
        
        readinessRepository.save(readiness);
    }
    
    /**
     * Finalize assessment: check all components, determine readiness.
     */
    public PreOpReadiness finalizeAssessment(UUID readinessId) {
        PreOpReadiness readiness = readinessRepository.findById(readinessId).orElseThrow();
        
        // Count required checks completed
        int requiredChecks = 11;  // All the hasXXX fields
        int completedChecks = countCompletedChecks(readiness);
        
        if (completedChecks < requiredChecks) {
            readiness.setStatus(PreOpReadinessStatus.PENDING_ASSESSMENT);
        } else if (!readiness.getHolds().isEmpty()) {
            readiness.setStatus(PreOpReadinessStatus.HOLD);
        } else {
            readiness.setStatus(PreOpReadinessStatus.READY);
        }
        
        return readinessRepository.save(readiness);
    }
    
    private int countCompletedChecks(PreOpReadiness r) {
        int count = 0;
        if (r.getHasConsultationApproval() != null && r.getHasConsultationApproval()) count++;
        if (r.getHasPreOpLabResults() != null && r.getHasPreOpLabResults()) count++;
        if (r.getHasNephropathyAssessment() != null && r.getHasNephropathyAssessment()) count++;
        if (r.getHasAnesthesiaConsent() != null && r.getHasAnesthesiaConsent()) count++;
        if (r.getHasMedicationReview() != null && r.getHasMedicationReview()) count++;
        if (r.getHasAllergyVerification() != null && r.getHasAllergyVerification()) count++;
        if (r.getHasPhysicalExamClearance() != null && r.getHasPhysicalExamClearance()) count++;
        if (r.getHasCoagulationProfile() != null && r.getHasCoagulationProfile()) count++;
        if (r.getHasCardiacClearance() != null && r.getHasCardiacClearance()) count++;
        if (r.getHasInfectionScreening() != null && r.getHasInfectionScreening()) count++;
        if (r.getHasNutritionAssessment() != null && r.getHasNutritionAssessment()) count++;
        return count;
    }
}
```

---

### 2.3 OperationExecution Entity + Scheduling Workflow (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/entity/OperationExecution.java`

**Implement**:
```java
@Entity
@Table(name = "operation_executions", indexes = {
    @Index(name = "idx_status_scheduled", columnList = "status, scheduledDateTime")
})
public class OperationExecution {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private UUID referralId;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Column(nullable = false)
    private UUID surgeonId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus status;  // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, ABORTED
    
    @Column(nullable = false)
    private LocalDateTime scheduledDateTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatingRoomType operatingRoom;
    
    // Pre-op checkin (on day of surgery)
    private LocalDateTime checkInTime;
    private Boolean preOpCheckinConfirmed;
    private String preOpNotes;
    
    // Actual operation timing
    private LocalDateTime operationStartTime;
    private LocalDateTime operationEndTime;
    
    private Integer operationDurationMinutes;  // computed
    private Boolean operationSuccessful;
    
    // Post-op
    private LocalDateTime postOpNotes;
    private UUID postOpReportId;
    
    // Cancellation
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    
    // getters/setters
}

public enum OperationStatus {
    SCHEDULED,    // Appointment set
    IN_PROGRESS,  // Pre-op checkin confirmed, operation started
    COMPLETED,    // Post-op report generated
    CANCELLED,    // Cancelled before start (by patient/surgeon)
    ABORTED       // Started but couldn't complete (emergency abort)
}
```

---

### 2.4 ReceptionistSchedulingService (1.5 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/service/ReceptionistSchedulingService.java`

**Implement receptionist workflow**:
```java
@Service
@Transactional
public class ReceptionistSchedulingService {
    
    @Autowired private OperationExecutionRepository operationRepository;
    @Autowired private SurgeryReferralRequestRepository referralRepository;
    @Autowired private PreOpReadinessRepository readinessRepository;
    @Autowired private SurgeonAvailabilityService availabilityService;
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    /**
     * Receptionist schedules operation after:
     * 1. Surgeon accepted referral
     * 2. Pre-op readiness = READY
     * 3. Surgeon + OR available at chosen time
     */
    public OperationExecution scheduleOperation(
            UUID referralId,
            LocalDateTime preferredDateTime,
            UUID receptionistId) {
        
        SurgeryReferralRequest referral = referralRepository.findById(referralId).orElseThrow();
        if (referral.getStatus() != ReferralStatus.ACCEPTED_BY_SURGEON) {
            throw new InvalidStateException("Referral not accepted by surgeon");
        }
        
        PreOpReadiness readiness = readinessRepository.findByReferralId(referralId).orElseThrow();
        if (readiness.getStatus() != PreOpReadinessStatus.READY) {
            throw new PreOpNotReadyException("Patient not ready: " + readiness.getStatus());
        }
        
        // Find available OR and surgeon availability
        SurgeonAvailabilityReport availability = availabilityService.getAvailability(
            referral.getAcceptingSurgeonId()
        );
        
        if (!availability.available()) {
            throw new SurgeonUnavailableException("Surgeon not available");
        }
        
        // Schedule operation
        OperationExecution operation = new OperationExecution();
        operation.setReferralId(referralId);
        operation.setPatientId(referral.getPatientId());
        operation.setSurgeonId(referral.getAcceptingSurgeonId());
        operation.setScheduledDateTime(preferredDateTime);
        operation.setOperatingRoom(OperatingRoomType.OR1);  // TODO: Find available OR
        operation.setStatus(OperationStatus.SCHEDULED);
        
        operation = operationRepository.save(operation);
        
        // Update referral
        referral.setScheduledOperationId(operation.getId());
        referral.setScheduledDateTime(preferredDateTime);
        referral.setStatus(ReferralStatus.SCHEDULED);
        referralRepository.save(referral);
        
        // Publish event
        eventPublisher.publishEvent(new OperationScheduledEvent(this, operation.getId(), 
                                   referral.getPatientId(), preferredDateTime));
        
        return operation;
    }
    
    /**
     * Receptionist confirms pre-op checkin on day of surgery.
     */
    public OperationExecution confirmPreOpCheckin(UUID operationId, String notes) {
        OperationExecution operation = operationRepository.findById(operationId).orElseThrow();
        
        if (operation.getStatus() != OperationStatus.SCHEDULED) {
            throw new InvalidStateException("Operation not in SCHEDULED state");
        }
        
        operation.setCheckInTime(LocalDateTime.now());
        operation.setPreOpCheckinConfirmed(true);
        operation.setPreOpNotes(notes);
        operation.setStatus(OperationStatus.IN_PROGRESS);
        
        return operationRepository.save(operation);
    }
}
```

---

### 2.5 Event Classes (1 hour)

Create in `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/event/`:

```java
public class PreOpReadinessRequestedEvent extends ApplicationEvent {
    private UUID referralId;
    private UUID patientId;
    // constructor, getters
}

public class PreOpLabsReadyEvent extends ApplicationEvent {
    private UUID referralId;
    private BigDecimal serumPotassium_mmolL;
    private BigDecimal serumCreatinine_umolL;
    // More fields from clinical-service
    // constructor, getters
}

public class PreOpHoldAlertEvent extends ApplicationEvent {
    private UUID referralId;
    private Set<PreOpHoldReason> holds;
    // constructor, getters
}

public class SurgeryOfferMadeEvent extends ApplicationEvent {
    private UUID referralId;
    private UUID surgeonId;
    private UUID patientId;
    // constructor, getters
}

public class SurgeryAcceptedEvent extends ApplicationEvent {
    private UUID referralId;
    private UUID surgeonId;
    private UUID patientId;
    // constructor, getters
}

public class SurgeryRejectedEvent extends ApplicationEvent {
    private UUID referralId;
    private UUID surgeonId;
    private String reason;
    // constructor, getters
}

public class OperationScheduledEvent extends ApplicationEvent {
    private UUID operationId;
    private UUID patientId;
    private LocalDateTime scheduledDateTime;
    // constructor, getters
}
```

---

## DAY 3: REST Endpoints + Post-Op Integration + Tests (7 hours)

### 3.1 REST Endpoints (2 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/controller/ProcedureController.java`

```
POST   /api/v1/surgery-referrals                              → create referral
GET    /api/v1/surgery-referrals/{id}                         → retrieve status
POST   /api/v1/surgery-referrals/{id}/claim-next-for-surgeon  → FIFO claim (atomic)
POST   /api/v1/surgery-referrals/{id}/accept-offer            → accept offer
POST   /api/v1/surgery-referrals/{id}/reject-offer            → reject offer
GET    /api/v1/surgeons/{id}/availability                     → availability report
POST   /api/v1/preop-readiness/{referralId}                   → start assessment
GET    /api/v1/preop-readiness/{referralId}                   → check status
POST   /api/v1/operations/{id}/schedule                       → receptionist schedule
POST   /api/v1/operations/{id}/checkin                        → pre-op checkin confirm
GET    /api/v1/operations/{id}                                → retrieve status
POST   /api/v1/operations/{id}/start                          → mark as IN_PROGRESS
POST   /api/v1/operations/{id}/complete                       → generate post-op report
```

---

### 3.2 Post-Op Integration (1.5 hours)

Create: `BackEnd/microservices/procedure-service/src/main/java/com/nephropaidi/procedure/service/PostOpIntegrationService.java`

**Implement**:
```java
@Service
@Transactional
public class PostOpIntegrationService {
    
    @Autowired private OperationExecutionRepository operationRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    /**
     * Surgeon completes operation → generate post-op report → publish event
     * Triggers:
     * - pharmacy-service: post-op medication orders
     * - clinical-service: post-op clinical notes
     * - medical dossier: add operation to timeline
     */
    public void completeOperation(
            UUID operationId,
            PostOpReportInput report,  // surgeon's post-op notes
            UUID surgeonId) {
        
        OperationExecution operation = operationRepository.findById(operationId).orElseThrow();
        
        if (operation.getStatus() != OperationStatus.IN_PROGRESS) {
            throw new InvalidStateException("Operation not in progress");
        }
        
        operation.setOperationEndTime(LocalDateTime.now());
        operation.setOperationDurationMinutes(
            (int) Duration.between(operation.getOperationStartTime(), 
                                  operation.getOperationEndTime()).toMinutes()
        );
        operation.setOperationSuccessful(report.isSuccessful());
        operation.setStatus(OperationStatus.COMPLETED);
        
        operation = operationRepository.save(operation);
        
        // Publish event with post-op report details
        eventPublisher.publishEvent(new PostOpReportCreatedEvent(
            this,
            operation.getReferralId(),
            operation.getPatientId(),
            surgeonId,
            report.getPostOpNotes(),
            report.getPostOpMedications(),  // pharmacy listens
            report.getDrains(),
            report.getFollowUpSchedule()
        ));
    }
}
```

---

### 3.3 Integration Tests (2.5 hours)

Create: `BackEnd/microservices/procedure-service/src/test/java/com/nephropaidi/procedure/integration/`

**12+ tests**:
1. Create referral → status = PENDING
2. Two surgeons try to claim same referral → only 1 succeeds (FIFO lock works)
3. Surgeon offers referral → status = OFFERED, offer expires 24h
4. Surgeon accepts offer → status = ACCEPTED_BY_SURGEON
5. Surgeon rejects offer → status back to PENDING
6. Surgeon availability: 3 operations this week, max 10 → available = true
7. Surgeon availability: 10 operations this week → available = false
8. Next available slot calculation: surgeon Mon-Fri 8-5 → finds slot Mon 8am
9. Create pre-op readiness → status = PENDING_ASSESSMENT
10. High K+ detected → pre-op hold added → status = HOLD
11. Receptionist schedules operation when ready → status = SCHEDULED
12. Receptionist checkin → status = IN_PROGRESS
13. Operation completed → PostOpReportCreatedEvent published
14. Two surgeons concurrently claim FIFO → OptimisticLockException handled

---

## COMPLETION CHECKLIST

- [ ] Day 1: SurgeryReferralRequest entity with FIFO ordering
- [ ] Day 1: FIFOSurgeryQueueService with pessimistic lock (atomic claiming)
- [ ] Day 1: SurgeonAvailabilityService (OR + surgeon scheduling calculation)
- [ ] Day 1: SurgeonSchedule entity + working days
- [ ] Day 2: PreOpReadiness entity + validator
- [ ] Day 2: PreOpReadinessService (assessment workflow)
- [ ] Day 2: OperationExecution entity (scheduling + execution)
- [ ] Day 2: ReceptionistSchedulingService (workflow gate)
- [ ] Day 2: Pre-op/post-op event wiring
- [ ] Day 3: 10 REST endpoints responding
- [ ] Day 3: Post-op integration working
- [ ] Day 3: 14+ integration tests passing
- [ ] Day 3: Swagger documentation generated

---

## CRITICAL RACE CONDITION PREVENTION

⚠️ **The FIFO lock is CRITICAL for correctness**:
- Must use `@Lock(LockModeType.PESSIMISTIC_WRITE)` or SQL `FOR UPDATE NOWAIT`
- Without it, multiple surgeons can claim same referral concurrently
- Test explicitly: 2 surgeons simultaneous claim → only 1 succeeds

---

## DEPENDENCY COORDINATION

✅ **Blocks Student A (Day 2)**: Pre-op event listeners  
✅ **Blocks Student B (Day 2)**: Pre-op/post-op medication events  
✅ **Consumes from Student A** (Day 2): PreOpLabsReadyEvent (from clinical-service)  
✅ **Consumes from Student B** (Day 2): Pre-op medication checks  
⏰ **Day 2 midday**: Receive PreOpReadinessRequestedEvent definition from Student A  
⏰ **Day 3 morning**: Publish PostOpReportCreatedEvent → Student B & A consume

---

**Start Day 1 at 09:00. Pessimistic locking is your friend! 🚀**
