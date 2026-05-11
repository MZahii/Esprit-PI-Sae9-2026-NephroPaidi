# STUDENT B: Pharmacy Service + Ops Service Integration

**Duration**: 3 Days × 7 hours/day = 21 hours  
**Services**: 
- `BackEnd/microservices/pharmacy-service` (primary)
- `BackEnd/microservices/ops-service` (secondary: pre-op/post-op nurse tasks)
- `BackEnd/microservices/ai-pharmacy-service` (monitoring)

**Objective**: Implement stock management with audit trail, pharmacist-enforced workflow (Request → Approval → Dispense), ledger immutability, and nurse task integration for pre-op/post-op workflows.

---

## DAY 1: Stock Management Foundation + Pharmacist Identity (7 hours)

### 1.1 Entity: StockItem + StockLedgerEntry (2 hours)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/entity/`

**StockItem.java**:
```java
@Entity
@Table(name = "stock_items")
public class StockItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String sku;  // Stock Keeping Unit
    
    @Column(nullable = false)
    private String medicationName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmaceuticalCategory category;  // NEPHROLOGY_SPECIFIC, BLOOD_PRODUCT, STANDARD, OTHER
    
    @Column(nullable = false)
    private Integer quantityInStock;  // immutable; queries apply ledger deltas
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus status;  // AVAILABLE, LOW_STOCK, CRITICAL, OUT_OF_STOCK, DISCONTINUED
    
    private Integer reorderLevel;
    private Integer reorderQuantity;
    
    @Column(nullable = false)
    private LocalDateTime lastInventoryCheck;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Transient computed field: current quantity from ledger
    @Transient
    private Integer computedQuantity;
    
    // getters/setters
}

public enum FarmaceuticalCategory {
    NEPHROLOGY_SPECIFIC,  // ACE inhibitors, ARBs, diuretics for renal patients
    BLOOD_PRODUCT,         // Packed RBC, FFP, platelets (separate ledger)
    STANDARD,              // Antibiotics, analgesics, etc.
    OTHER
}

public enum StockStatus {
    AVAILABLE, LOW_STOCK, CRITICAL, OUT_OF_STOCK, DISCONTINUED
}
```

**StockLedgerEntry.java** (immutable audit log):
```java
@Entity
@Table(name = "stock_ledger_entries")
public class StockLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID stockItemId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;  // IN (receipt), OUT (dispense), ADJUSTMENT, RETURN, DAMAGE, EXPIRED
    
    @Column(nullable = false)
    private Integer quantity;  // signed; positive=in, negative=out
    
    @Column(nullable = false)
    private UUID performedByPharmacistId;  // WHO did this (enforce pharmacist identity)
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String reason;  // e.g., "Dispense for Rx #123", "Received batch #ABC", "Expired 2026-05-10"
    
    @Column(nullable = false)
    private UUID relatedDocumentId;  // e.g., PrescriptionId, ReceiptId
    
    @Enumerated(EnumType.STRING)
    private DocumentType relatedDocumentType;  // PRESCRIPTION, MEDICATION_REQUEST, RECEIPT, ADJUSTMENT, DAMAGE_REPORT
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // NO updates/deletes allowed; only inserts
    // Use SQL trigger or JPA @PrePersist to enforce immutability
}

public enum LedgerEntryType {
    IN, OUT, ADJUSTMENT, RETURN, DAMAGE, EXPIRED
}

public enum DocumentType {
    PRESCRIPTION, MEDICATION_REQUEST, RECEIPT, ADJUSTMENT, DAMAGE_REPORT
}
```

---

### 1.2 StockLedgerService: Immutable Append-Only Log (1.5 hours)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/service/StockLedgerService.java`

**Implement**:
```java
@Service
@Transactional
public class StockLedgerService {
    
    @Autowired private StockLedgerRepository ledgerRepository;
    @Autowired private StockItemRepository stockRepository;
    
    /**
     * Record a stock movement (immutable append).
     * CRITICAL: Throws exception if pharmacistId not authenticated.
     */
    public StockLedgerEntry recordMovement(
            UUID stockItemId,
            LedgerEntryType entryType,
            Integer quantity,
            UUID performedByPharmacistId,  // from SecurityContext
            String reason,
            UUID relatedDocumentId,
            DocumentType relatedDocumentType) {
        
        // Validate pharmacist identity (must be in current SecurityContext)
        validatePharmacistIdentity(performedByPharmacistId);
        
        // Create immutable entry
        StockLedgerEntry entry = new StockLedgerEntry();
        entry.setStockItemId(stockItemId);
        entry.setEntryType(entryType);
        entry.setQuantity(quantity);
        entry.setPerformedByPharmacistId(performedByPharmacistId);
        entry.setTimestamp(LocalDateTime.now());
        entry.setReason(reason);
        entry.setRelatedDocumentId(relatedDocumentId);
        entry.setRelatedDocumentType(relatedDocumentType);
        
        StockLedgerEntry saved = ledgerRepository.save(entry);
        
        // Update stock status (trigger low_stock alert if needed)
        updateStockStatus(stockItemId);
        
        return saved;
    }
    
    /**
     * Query total stock for item by summing ledger entries.
     */
    public Integer queryCurrentStock(UUID stockItemId) {
        return ledgerRepository.findByStockItemId(stockItemId).stream()
            .mapToInt(StockLedgerEntry::getQuantity)
            .sum();
    }
    
    /**
     * Get full ledger history for item (audit trail).
     */
    public List<StockLedgerEntry> getLedgerHistory(UUID stockItemId) {
        return ledgerRepository.findByStockItemIdOrderByTimestampDesc(stockItemId);
    }
    
    /**
     * Validate that performedByPharmacistId matches current SecurityContext.
     */
    private void validatePharmacistIdentity(UUID pharmacistId) {
        // TODO: Implement using Spring Security + Keycloak
        // Pseudo-code:
        // UUID currentUserId = getCurrentAuthenticatedUserId();
        // UserRole role = getCurrentUserRole();
        // if (!role.equals(UserRole.PHARMACIST) || !currentUserId.equals(pharmacistId)) {
        //     throw new UnauthorizedPharmacistException(...);
        // }
    }
    
    private void updateStockStatus(UUID stockItemId) {
        StockItem item = stockRepository.findById(stockItemId).orElseThrow();
        Integer current = queryCurrentStock(stockItemId);
        
        if (current <= 0) {
            item.setStatus(StockStatus.OUT_OF_STOCK);
        } else if (current < item.getReorderLevel()) {
            item.setStatus(StockStatus.CRITICAL);
        } else if (current < item.getReorderLevel() * 1.5) {
            item.setStatus(StockStatus.LOW_STOCK);
        } else {
            item.setStatus(StockStatus.AVAILABLE);
        }
        
        stockRepository.save(item);
    }
}
```

---

### 1.3 BloodProductLedger (Separate Tracking) (1 hour)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/entity/BloodProductLedger.java`

**Implement**:
```java
@Entity
@Table(name = "blood_product_ledger")
public class BloodProductLedger {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodProductType productType;  // PACKED_RBC, FFP, PLATELETS, CRYOPRECIPITATE, WHOLE_BLOOD
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup bloodGroup;  // O_POSITIVE, O_NEGATIVE, A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE
    
    @Column(nullable = false)
    private Integer quantityInUnits;  // units (e.g., 2 units RBC = 1 bag)
    
    @Column(nullable = false)
    private LocalDate expirationDate;
    
    @Column(nullable = false)
    private UUID recordedByPharmacistId;
    
    @Column(nullable = false)
    private LocalDateTime recordedAt;
    
    @Column(columnDefinition = "TEXT")
    private String donorInfo;  // references DonorType enum from clinical-service
    
    private UUID dispensedToPatientId;
    private UUID dispensedByPharmacistId;
    private LocalDateTime dispensedAt;
    
    @Enumerated(EnumType.STRING)
    private BloodProductStatus status;  // IN_STOCK, DISPENSED, EXPIRED, DAMAGED, RETURNED
    
    // getters/setters
}

public enum BloodProductType {
    PACKED_RBC, FFP, PLATELETS, CRYOPRECIPITATE, WHOLE_BLOOD
}

public enum BloodGroup {
    O_POSITIVE, O_NEGATIVE, A_POSITIVE, A_NEGATIVE, 
    B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE
}

public enum BloodProductStatus {
    IN_STOCK, DISPENSED, EXPIRED, DAMAGED, RETURNED
}
```

---

### 1.4 Pharmacist Identity Enforcement (0.5 hour)

Add to `pharmacy-service/src/main/java/com/nephropaidi/pharmacy/security/`:

**PharmacistAuthenticationFilter.java**:
```java
@Component
public class PharmacistAuthenticationFilter {
    
    @Autowired private JwtTokenProvider tokenProvider;
    
    /**
     * Gate: Ensure request contains valid pharmacist token.
     * Extracts pharmacistId and role from JWT.
     */
    public UUID getCurrentPharmacistId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        
        // Extract pharmacistId from claims
        Claims claims = tokenProvider.getClaimsFromToken((String) auth.getCredentials());
        String role = claims.get("role", String.class);
        
        if (!"PHARMACIST".equals(role)) {
            throw new ForbiddenException("User is not a pharmacist");
        }
        
        return UUID.fromString(claims.getSubject());  // user ID as subject
    }
}
```

---

### 1.5 Medication Request Entity (0.5 hour)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/entity/MedicationRequest.java`

**Stub for now** (will expand Day 2):
```java
@Entity
@Table(name = "medication_requests")
public class MedicationRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Column(nullable = false)
    private UUID stockItemId;
    
    @Column(nullable = false)
    private Integer requestedQuantity;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicationRequestStatus status;  // PENDING, APPROVED, REJECTED, FULFILLED
    
    @Column(nullable = false)
    private UUID requestingPhysicianId;
    
    private UUID approvingPharmacistId;
    private String approvalJustification;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime approvedAt;
    private LocalDateTime fulfilledAt;
    
    // getters/setters
}

public enum MedicationRequestStatus {
    PENDING, APPROVED, REJECTED, FULFILLED
}
```

---

## DAY 2: Workflow (Request → Approval → Dispense) + Pre-Op/Post-Op Integration (7 hours)

### 2.1 MedicationRequestService (2 hours)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/service/MedicationRequestService.java`

**Implement workflow state machine**:
```java
@Service
@Transactional
public class MedicationRequestService {
    
    @Autowired private MedicationRequestRepository requestRepository;
    @Autowired private StockLedgerService ledgerService;
    @Autowired private PharmacistAuthenticationFilter authFilter;
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    /**
     * Step 1: Physician creates medication request.
     */
    public MedicationRequest createRequest(UUID patientId, UUID stockItemId, 
                                          Integer requestedQuantity, UUID physicianId) {
        MedicationRequest req = new MedicationRequest();
        req.setPatientId(patientId);
        req.setStockItemId(stockItemId);
        req.setRequestedQuantity(requestedQuantity);
        req.setRequestingPhysicianId(physicianId);
        req.setStatus(MedicationRequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        
        return requestRepository.save(req);
    }
    
    /**
     * Step 2: Pharmacist approves or rejects request.
     * GATE: Pharmacist identity enforced.
     */
    public MedicationRequest approveRequest(UUID requestId, String justification) {
        UUID pharmacistId = authFilter.getCurrentPharmacistId();
        
        MedicationRequest req = requestRepository.findById(requestId).orElseThrow();
        if (req.getStatus() != MedicationRequestStatus.PENDING) {
            throw new InvalidStateException("Request already processed");
        }
        
        // Validate stock availability
        Integer available = ledgerService.queryCurrentStock(req.getStockItemId());
        if (available < req.getRequestedQuantity()) {
            // Reject instead of approve
            req.setStatus(MedicationRequestStatus.REJECTED);
            req.setApprovalJustification("Insufficient stock: " + available + " available, " 
                                        + req.getRequestedQuantity() + " requested");
            requestRepository.save(req);
            
            eventPublisher.publishEvent(new MedicationRequestRejectedEvent(this, requestId, 
                                       "Insufficient stock"));
            return req;
        }
        
        // Approve
        req.setStatus(MedicationRequestStatus.APPROVED);
        req.setApprovingPharmacistId(pharmacistId);
        req.setApprovalJustification(justification);
        req.setApprovedAt(LocalDateTime.now());
        
        requestRepository.save(req);
        
        eventPublisher.publishEvent(new MedicationRequestApprovedEvent(this, requestId, 
                                   pharmacistId));
        
        return req;
    }
    
    /**
     * Step 3: Pharmacist dispenses medication (reserves stock, creates ledger entry).
     * GATE: Pharmacist identity enforced. Request must be APPROVED.
     */
    public MedicationRequest dispenseRequest(UUID requestId) {
        UUID pharmacistId = authFilter.getCurrentPharmacistId();
        
        MedicationRequest req = requestRepository.findById(requestId).orElseThrow();
        if (req.getStatus() != MedicationRequestStatus.APPROVED) {
            throw new InvalidStateException("Request not approved for dispensing");
        }
        
        // Create immutable ledger entry (OUT)
        ledgerService.recordMovement(
            req.getStockItemId(),
            LedgerEntryType.OUT,
            -req.getRequestedQuantity(),  // negative for OUT
            pharmacistId,
            "Dispense for patient " + req.getPatientId(),
            requestId,
            DocumentType.MEDICATION_REQUEST
        );
        
        // Mark request as fulfilled
        req.setStatus(MedicationRequestStatus.FULFILLED);
        req.setFulfilledAt(LocalDateTime.now());
        requestRepository.save(req);
        
        eventPublisher.publishEvent(new MedicationDispensedEvent(this, requestId, 
                                   req.getPatientId(), pharmacistId));
        
        return req;
    }
}
```

---

### 2.2 Pre-Op/Post-Op Nurse Task Integration (1.5 hours)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/event/PreOpPostOpEventListener.java`

**Implement event listeners** (consume from ops-service via event bus):
```java
@Component
public class PreOpPostOpEventListener {
    
    @Autowired private MedicationRequestService medicationService;
    @Autowired private StockLedgerService ledgerService;
    @Autowired private PharmacistAuthenticationFilter authFilter;
    
    /**
     * When ops-service publishes PreOpReadinessRequestedEvent,
     * pharmacy-service checks if pre-op medications are in stock.
     */
    @EventListener
    public void onPreOpReadinessRequested(PreOpReadinessRequestedEvent event) {
        UUID patientId = event.getPatientId();
        UUID procedureId = event.getProcedureId();
        
        // Look up pre-op medication protocol for patient (e.g., antibiotics, anticoagulation)
        List<PreOpMedicationProtocol> protocols = lookupPreOpProtocols(patientId);
        
        for (PreOpMedicationProtocol protocol : protocols) {
            Integer available = ledgerService.queryCurrentStock(protocol.getStockItemId());
            if (available < protocol.getRequiredQuantity()) {
                // Publish alert to ops-service
                publishPreOpMedicationUnavailableEvent(patientId, procedureId, 
                                                      protocol.getMedicationName());
            }
        }
    }
    
    /**
     * When ops-service publishes PostOpReportCreatedEvent,
     * pharmacy-service receives post-op medication orders.
     */
    @EventListener
    public void onPostOpReportCreated(PostOpReportCreatedEvent event) {
        UUID patientId = event.getPatientId();
        UUID procedureId = event.getProcedureId();
        
        // Extract post-op medication orders from report
        List<PostOpMedicationOrder> orders = event.getPostOpMedications();
        
        for (PostOpMedicationOrder order : orders) {
            // Auto-create medication request for each post-op medication
            UUID physicianId = event.getSurgeonId();  // surgeon as ordering physician
            
            MedicationRequest req = medicationService.createRequest(
                patientId,
                order.getStockItemId(),
                order.getQuantity(),
                physicianId
            );
            
            // Auto-approve if stock available (gate: system service account, not pharmacist)
            try {
                medicationService.approveRequest(req.getId(), 
                    "Auto-approved from post-op protocol");
            } catch (Exception e) {
                // Escalate to pharmacist review
                publishPostOpMedicationApprovalNeededEvent(patientId, procedureId, 
                                                          order.getMedicationName());
            }
        }
    }
}
```

---

### 2.3 Pre-Op Medication Protocol Lookup (0.5 hour)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/entity/PreOpMedicationProtocol.java`

**Implement**:
```java
@Entity
@Table(name = "preop_medication_protocols")
public class PreOpMedicationProtocol {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID stockItemId;
    
    @Column(nullable = false)
    private String medicationName;
    
    @Column(nullable = false)
    private Integer requiredQuantity;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcedureType applicableProcedureType;  // NEPHROSTOMY, RENAL_BIOPSY, HEMODIALYSIS_FISTULA, TRANSPLANT_EVALUATION, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgeGroup applicableAgeGroup;  // NEONATAL, INFANT, CHILD, ADOLESCENT
    
    @Column(columnDefinition = "TEXT")
    private String protocolDescription;
    
    // getters/setters
}

public enum ProcedureType {
    NEPHROSTOMY, RENAL_BIOPSY, HEMODIALYSIS_FISTULA, TRANSPLANT_EVALUATION, 
    UROLOGICAL_PROCEDURE, SPINAL_PROCEDURE, SURGERY
}

public enum AgeGroup {
    NEONATAL, INFANT, CHILD, ADOLESCENT
}
```

---

### 2.4 Post-Op Medication Order Handling (1 hour)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/entity/PostOpMedicationOrder.java`

**Implement**:
```java
@Entity
@Table(name = "postop_medication_orders")
public class PostOpMedicationOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Column(nullable = false)
    private UUID procedureId;
    
    @Column(nullable = false)
    private UUID stockItemId;
    
    @Column(nullable = false)
    private String medicationName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteOfAdministration route;  // from clinical-service enum
    
    @Column(nullable = false)
    private UUID prescribingPhysicianId;  // surgeon
    
    @Column(nullable = false)
    private LocalDateTime prescribedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicationRequestStatus requestStatus;
    
    private LocalDateTime dispensedAt;
    
    // getters/setters
}
```

---

### 2.5 Event Classes for Pharmacy (1 hour)

Create in `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/event/`:

**Pharmacy-to-Ops Events**:
```java
public class PreOpMedicationUnavailableEvent extends ApplicationEvent {
    private UUID patientId;
    private UUID procedureId;
    private String medicationName;
    // constructor, getters
}

public class PostOpMedicationApprovalNeededEvent extends ApplicationEvent {
    private UUID patientId;
    private UUID procedureId;
    private String medicationName;
    // constructor, getters
}

public class MedicationRequestApprovedEvent extends ApplicationEvent {
    private UUID requestId;
    private UUID pharmacistId;
    // constructor, getters
}

public class MedicationDispensedEvent extends ApplicationEvent {
    private UUID requestId;
    private UUID patientId;
    private UUID pharmacistId;
    // constructor, getters
}

public class MedicationRequestRejectedEvent extends ApplicationEvent {
    private UUID requestId;
    private String reason;
    // constructor, getters
}
```

---

## DAY 3: REST Endpoints + Tests (7 hours)

### 3.1 REST Endpoints (2 hours)

Create: `BackEnd/microservices/pharmacy-service/src/main/java/com/nephropaidi/pharmacy/controller/PharmacyController.java`

```
POST   /api/v1/medication-requests                    → create + emit event
GET    /api/v1/medication-requests/{id}               → retrieve status
POST   /api/v1/medication-requests/{id}/approve       → approve (pharmacist-only gate)
POST   /api/v1/medication-requests/{id}/reject        → reject (pharmacist-only gate)
POST   /api/v1/medication-requests/{id}/dispense      → dispense (pharmacist-only gate)
GET    /api/v1/stock-items                            → list all stock with current qty
GET    /api/v1/stock-items/{id}/ledger                → audit trail
POST   /api/v1/blood-products                         → record blood product receipt
GET    /api/v1/blood-products?bloodGroup=O_POSITIVE   → query by group
GET    /api/v1/stock-items/{id}/status                → low stock alerts
```

---

### 3.2 Pharmacist Gate Tests (2 hours)

Create integration tests:
```
✓ Unauthorized physician cannot approve/dispense → ForbiddenException
✓ Pharmacist can approve → status changes to APPROVED
✓ Pharmacist can dispense when approved → ledger entry created, stock decremented
✓ Cannot dispense without approval → InvalidStateException
✓ Insufficient stock → approval rejected automatically
✓ Blood product tracking → recorded and queried by blood group
✓ Pre-op integration → PreOpReadinessRequested event triggers medication check
✓ Post-op integration → PostOpReportCreated event triggers auto-approval
```

---

### 3.3 Stock Ledger Immutability Tests (1.5 hours)

```
✓ Ledger entries cannot be updated (JPA protection)
✓ Ledger entries cannot be deleted (JPA protection)
✓ Ledger sum matches current stock query
✓ Pharmacist identity enforced on each entry
✓ Audit trail is queryable and complete
✓ Stock status auto-updates (AVAILABLE → LOW_STOCK → CRITICAL → OUT_OF_STOCK)
```

---

### 3.4 Workflow State Machine Tests (1.5 hours)

```
✓ Request → PENDING on creation
✓ Request → APPROVED when pharmacist approves + stock sufficient
✓ Request → REJECTED when stock insufficient
✓ Request → FULFILLED when pharmacist dispenses
✓ Cannot re-approve already processed request
✓ Cannot dispense non-approved request
✓ Pre-op event listener publishes alert when medication unavailable
✓ Post-op event listener auto-creates and approves medication requests
```

---

## COMPLETION CHECKLIST

- [ ] Day 1: StockItem + StockLedgerEntry entities
- [ ] Day 1: Immutable ledger service (append-only)
- [ ] Day 1: BloodProductLedger (separate tracking)
- [ ] Day 1: Pharmacist identity gate enforced
- [ ] Day 1: MedicationRequest stub entity
- [ ] Day 2: MedicationRequestService state machine (PENDING → APPROVED → FULFILLED)
- [ ] Day 2: Pre-op/post-op event listeners wired
- [ ] Day 2: PreOpMedicationProtocol entity + lookup
- [ ] Day 2: PostOpMedicationOrder entity
- [ ] Day 2: Pharmacy event classes
- [ ] Day 3: 8 REST endpoints responding
- [ ] Day 3: Pharmacist gate passing tests
- [ ] Day 3: Stock ledger immutability tests passing
- [ ] Day 3: Workflow state machine tests passing

---

## DEPENDENCY COORDINATION

✅ **Blocked by Student A (Day 1)**: None (pharmacy-service is independent)  
✅ **Blocks Student C (Day 2)**: Pre-op/post-op event listeners → ops-service listens on Day 2-3  
⏰ **Day 2 midday**: Receive event definitions from Student A (MedicationAtDischarge, FollowUpPlan)  
⏰ **Day 3**: Publish PreOpMedicationUnavailable + PostOpMedicationApprovalNeeded events → Student C consumes

---

**Start Day 1 at 09:00. Stock validation is critical! 🚀**
