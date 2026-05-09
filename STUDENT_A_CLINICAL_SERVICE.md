# STUDENT A: Clinical-Service Pediatric Nephrology

**Duration**: 3 Days × 7 hours/day = 21 hours  
**Service**: `BackEnd/microservices/clinical-service`  
**Objective**: Implement pediatric nephrology enrichment (65+ enums, 7 entities, Schwartz eGFR calculator, HAS discharge validator, medical dossier, 11 REST endpoints)

---

## DAY 1: Enums + Schwartz + Dossier Foundation (7 hours)

### 1.1 Enum Generation (4 hours) — Generate in 6 batches

Create each enum in: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/enums/`

**Batch 1 (30 min) — Core Renal**
```
CKDStage.java          // STAGE_1, STAGE_2, STAGE_3A, STAGE_3B, STAGE_4, STAGE_5, STAGE_5D
CKDCause.java          // CAKUT, GLOMERULONEPHRITIS, HEREDITARY_NEPHROPATHY, IGA_NEPHROPATHY, ALPORT_SYNDROME, FSGS, LUPUS_NEPHRITIS, HUS, REFLUX_NEPHROPATHY, POLYCYSTIC_KIDNEY, TUBULOPATHY, ANCA_VASCULITIS, SYSTEMIC_DISEASE, OTHER, UNKNOWN
CAKUTSubtype.java      // RENAL_AGENESIS_UNILATERAL, RENAL_AGENESIS_BILATERAL, RENAL_HYPOPLASIA, RENAL_DYSPLASIA, MULTICYSTIC_DYSPLASTIC_KIDNEY, HORSESHOE_KIDNEY, ADPKD, ARPKD, VESICOURETERAL_REFLUX, UPJ_OBSTRUCTION, MEGAURETER, POSTERIOR_URETHRAL_VALVES, ISOLATED_CYST
ProteinuriaCategory.java  // ABSENT, TRACE, MILD, NEPHROTIC_RANGE
HematuriaLevel.java       // ABSENT, MICROSCOPIC, MACROSCOPIC
RRTType.java              // HEMODIALYSIS, PERITONEAL_DIALYSIS, CRRT, TRANSPLANT
```

**Batch 2 (30 min) — Nephropathology**
```
NephroticCause.java           // MINIMAL_CHANGE, MEMBRANOPROLIFERATIVE, FSGS, MEMBRANOUS, HSP, LUPUS, HBV_HCV, HIV, OTHER
CorticosteroidResponse.java   // SENSITIVE, RESISTANT, DEPENDENT, FREQUENT_RELAPSER
NephroticEvolution.java       // DEFINITIVE_REMISSION, SPACED_RELAPSES, STEROID_DEPENDENT
HUSType.java                  // TYPICAL_STEC, ATYPICAL
HUSOutcome.java               // RECOVERY, RESIDUAL_HTA, CHRONIC_RENAL_FAILURE, FATAL
AKIOrigin.java                // PRE_RENAL, INTRINSIC_RENAL, POST_RENAL
DialysisIndication.java       // OLIGURIA, FLUID_OVERLOAD, SEVERE_ELECTROLYTE_DISTURBANCE, BUN_OVER_80_MG_DL
```

**Batch 3 (30 min) — Hereditary & Tubular (Stub for now, expand later)**
```
HereditaryNephropathyType.java  // ALPORT_SYNDROME, NPHS1, NPHS2, DENYS_DRASH, FRASIER, WAGR, RENAL_COLOBOMA_PAX2, CYSTS_DIABETES_TCF2, TUBEROUS_SCLEROSIS, LAURENCE_MOON_BARDET_BIEDL, HYPEROXALURIA_TYPE1, CYSTINOSIS, NEPHRONOPHTHISIS, FABRY, GLYCOGENOSIS, ARPKD, ADPKD
TubulopathyType.java            // BARTTER, GITELMAN, LIDDLE, DENT_DISEASE, FANCONI_SYNDROME, HYPOPHOSPHATEMIC_RICKETS, DISTAL_RTA, PROXIMAL_RTA, PSEUDO_BARTTER
VURGrade.java                   // NONE, GRADE_1, GRADE_2, GRADE_3, GRADE_4, GRADE_5
```

**Batch 4 (20 min) — Vital Sign Classification**
```
HTASeverity.java   // NORMAL, HIGH_NORMAL, MODERATE, SEVERE, THREATENING
HTACause.java      // RENAL_PARENCHYMAL, RENAL_ARTERY_STENOSIS, RENAL_TUMOR, COARCTATION_AORTA, PHEOCHROMOCYTOMA, NEUROBLASTOMA, ADRENAL_HYPERPLASIA, CUSHING, ESSENTIAL
```

**Batch 5 (40 min) — Neonatal & Birth**
```
GestationalAgeCategory.java    // EXTREMELY_PREMATURE, VERY_PREMATURE, PREMATURE, TERM, POST_TERM
DeliveryMode.java              // NORMAL_VAGINAL, INSTRUMENTAL_VAGINAL, CSECTION_PRE_LABOR, CSECTION_IN_LABOR
PregnancyType.java             // SPONTANEOUS, IVF, AI, STIMULATION, OTHER, UNKNOWN
IVHGrade.java                  // NONE, GRADE_1, GRADE_2, GRADE_3, GRADE_4
BPDSeverity.java               // NONE, MILD, MODERATE, SEVERE
ROP_Stage.java                 // NONE, STAGE_1, STAGE_2, STAGE_2_PLUS, STAGE_3, STAGE_3_PLUS, STAGE_4
ROP_Treatment.java             // NONE, LASER, INTRAVITREAL_INJECTION, OTHER
RespiratoryPathologyType.java  // HMD, TTN, PPHN, MECONIUM_ASPIRATION, OTHER
CardiacPathologyType.java      // SEVERE_HEMODYNAMIC_DISORDER, CONGENITAL_HEART_DISEASE, PATENT_DUCTUS_ARTERIOSUS, VSD, ASD, LARGE_PFO, OTHER
PDA_Treatment.java             // MEDICAL, SURGICAL, ENDOVASCULAR, NONE
InfectiousAgent.java           // E_COLI, STREP_B, ENTEROBACTER, CANDIDA, CMV, OTHER
MaternalFetalInfection.java    // NONE, YES_WITHOUT_MENINGITIS, YES_WITH_MENINGITIS
Surfactant.java                // NOT_DONE, ONE_DOSE, TWO_DOSES, MORE_THAN_TWO, UNKNOWN
Hypotrophy.java                // NONE, BELOW_10TH_PERCENTILE, BELOW_3RD_PERCENTILE
HearingScore.java              // NORMAL_0, DOUBTFUL_1, PATHOLOGICAL_2
HearingTechnique.java          // AUTOMATED_ABR, OAE, THRESHOLD_ABR
HearingResult.java             // NORMAL, INCONCLUSIVE_UNILATERAL, INCONCLUSIVE_BILATERAL, NOT_COMMUNICATED
VisionScore.java               // NORMAL_0, DOUBTFUL_1, PATHOLOGICAL_2
NeurologyScore.java            // NORMAL_0, DOUBTFUL_1, PATHOLOGICAL_2
```

**Batch 6 (20 min) — Medications & Discharge**
```
MedicationStatus.java          // CONTINUED, MODIFIED, NEW, STOPPED
RouteOfAdministration.java     // ORAL, IV, SC, IM, INHALED, TOPICAL, RECTAL, NASAL
DischargeDestination.java      // HOME, DECEASED, OTHER_PEDIATRIC_WARD, HAD, TRANSFER, OTHER
CRHDocumentStatus.java         // COMPLETE, PARTIAL_PENDING_8_DAYS
AdmissionMode.java             // SCHEDULED, EMERGENCY, TRANSFER, REFERRAL
AllergyType.java               // DRUG, FOOD, ENVIRONMENTAL, LATEX, CONTRAST_AGENT, OTHER
AllergySeverity.java           // LOW, MODERATE, HIGH, ANAPHYLAXIS
AllergyStatus.java             // ACTIVE, INACTIVE, CHRONIC, INTERMITTENT, RECURRENT, RESOLVED
FeedingType.java               // BREASTFEEDING, MIXED, ADAPTED_FORMULA, ENTERAL_TUBE, HYPOPROTIDIC_PRODUCTS, UNKNOWN
DonorType.java                 // LIVING_RELATED, LIVING_UNRELATED, DECEASED
NephrotoxicDrug.java           // NSAIDS, ACE_INHIBITORS, AMINOGLYCOSIDES, CEPHALOSPORINS, CIPROFLOXACIN, ACYCLOVIR, AMPHOTERICIN, CONTRAST_AGENTS, CISPLATIN, IFOSFAMIDE, CICLOSPORIN, CARBAMAZEPINE, VALPROATE, IV_IMMUNOGLOBULINS, LITHIUM
KidneyEchogenicity.java        // NORMAL, MILDLY_INCREASED, HYPERECHOGENIC
CorticomedullaryDiff.java      // PRESERVED, REDUCED, ABOLISHED
BiopsyHistology.java           // MINIMAL_CHANGE, FSGS, MEMBRANOPROLIFERATIVE, MEMBRANOUS, IGA_NEPHROPATHY, MESANGIAL_SCLEROSIS, CORTICAL_NECROSIS, MICROANGIOPATHY, OTHER
NutritionalAssessment.java     // NORMAL, AT_RISK, MODERATE_MALNUTRITION, SEVERE_MALNUTRITION
EdemaLocation.java             // PERIORBITAL, LOWER_LIMB, ASCITES, GENERALIZED
ConsultationType.java          // CRC, CRH, CRO, CREO, CRMO, CRIO
```

**✅ Checkpoint**: All 65+ enums should compile without errors. Test by running `mvn clean compile`.

---

### 1.2 PediatricNephrologyRecord Entity Skeleton (1.5 hours)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/entity/PediatricNephrologyRecord.java`

**Implement**:
- `@Embeddable` or `@Entity` (discuss with team; recommend `@Embeddable` for embedding in other entities)
- **Renal function fields**:
  - `eGFR: BigDecimal` (computed)
  - `schwartz_k: BigDecimal` (0.33, 0.45, 0.55, 0.70)
  - `serumCreatinine_umolL: BigDecimal`
  - `serumCreatinine_mgdL: BigDecimal`
  - `cystatinC_mgL: BigDecimal`
  - `ckdStage: CKDStage` (enum)
  - `ckdCause: CKDCause` (enum)
- **Urinalysis**:
  - `urineProteinCreatinineRatio: BigDecimal`
  - `proteinuriaCategory: ProteinuriaCategory` (enum)
  - `hematuria: HematuriaLevel` (enum)
  - `dysmorphicErythrocytes: Boolean`
  - `leukocyturia: Boolean`
- **Biomarkers minimum**:
  - `serumAlbumin_g_L: BigDecimal`
  - `serumSodium_mmolL: BigDecimal`
  - `serumPotassium_mmolL: BigDecimal`
  - `serumBicarbonate_mmolL: BigDecimal`
  - `serumPhosphate_mmolL: BigDecimal`
  - `serumCalcium_mmolL: BigDecimal`
  - `pth_pg_mL: BigDecimal`
  - `vitaminD_25OH_nmolL: BigDecimal`
  - `serumUrea_mmolL: BigDecimal`
- **Placeholder sections** (stubs for Day 2 expansion):
  - `husPresent: Boolean` with `husType: HUSType` (defer HUS section)
  - `akiPresent: Boolean` with `akiOrigin: AKIOrigin` (defer AKI section)
  - `nephroticSyndrome: Boolean` (defer details)
  - `hereditaryNephropathy: Boolean` (defer details)

**✅ Checkpoint**: Entity compiles, can be embedded into ConsultationRecord (test with `@Embedded` annotation).

---

### 1.3 SchwartzGFRCalculator Service (1.5 hours)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/service/SchwartzGFRCalculator.java`

**Implement**:
```java
@Service
public class SchwartzGFRCalculator {
    
    public SchwartzResult calculate(BigDecimal heightCm, BigDecimal creatinineMgdL, 
                                   Integer ageYears, Boolean isPremature) {
        // Validate inputs
        if (heightCm == null || creatinineMgdL == null || ageYears == null) {
            throw new IllegalArgumentException("Height, creatinine, and age are required");
        }
        
        // Select k-value based on age
        BigDecimal k;
        if (isPremature != null && isPremature) {
            k = new BigDecimal("0.33");
        } else if (ageYears < 1) {
            k = new BigDecimal("0.45");  // term neonate
        } else if (ageYears < 13) {
            k = new BigDecimal("0.55");  // child
        } else {
            k = new BigDecimal("0.70");  // adolescent
        }
        
        // Compute eGFR = k × height_cm / creatinine_mg_dL
        BigDecimal eGFR = k.multiply(heightCm).divide(creatinineMgdL, 1, RoundingMode.HALF_UP);
        
        // Auto-compute CKD stage from eGFR
        CKDStage stage = computeStage(eGFR);
        
        return new SchwartzResult(eGFR, k, stage);
    }
    
    private CKDStage computeStage(BigDecimal eGFR) {
        if (eGFR.compareTo(new BigDecimal("90")) >= 0) return CKDStage.STAGE_1;
        if (eGFR.compareTo(new BigDecimal("60")) >= 0) return CKDStage.STAGE_2;
        if (eGFR.compareTo(new BigDecimal("45")) >= 0) return CKDStage.STAGE_3A;
        if (eGFR.compareTo(new BigDecimal("30")) >= 0) return CKDStage.STAGE_3B;
        if (eGFR.compareTo(new BigDecimal("15")) >= 0) return CKDStage.STAGE_4;
        return CKDStage.STAGE_5;
    }
}
```

**Create inner class**: `SchwartzResult` (record or simple POJO)
```java
public record SchwartzResult(BigDecimal eGFR, BigDecimal k, CKDStage stage) {}
```

**Test with 3 scenarios**:
1. **Neonate (premature)**: height=45cm, creatinine=0.6 mg/dL, age=0 → k=0.33 → eGFR=24.75
2. **Child**: height=100cm, creatinine=1.0 mg/dL, age=7 → k=0.55 → eGFR=55
3. **Adolescent**: height=160cm, creatinine=1.0 mg/dL, age=15 → k=0.70 → eGFR=112

**✅ Checkpoint**: Service compiles, calculator works, tests pass.

---

### 1.4 MedicalDossier Entity + Event Listeners (1 hour)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/entity/MedicalDossierEntry.java`

**Implement**:
```java
@Entity
@Table(name = "medical_dossier_entries")
public class MedicalDossierEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID patientId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;  // CONSULTATION, LAB, HOSPITALIZATION, PROCEDURE, DISCHARGE, REPORT
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private UUID sourceServiceId;  // which microservice published this entry
    
    // getters/setters
}

public enum EntryType {
    CONSULTATION, LAB, HOSPITALIZATION, PROCEDURE, DISCHARGE, PRE_OP_REPORT, POST_OP_REPORT
}
```

**Create event listener stubs** in a new `@Component EventListener`:
```java
@Component
public class MedicalDossierEventListener {
    
    @Autowired private MedicalDossierRepository dossierRepository;
    
    @EventListener
    public void onConsultationClosed(ConsultationClosedEvent event) {
        // TODO: Create dossier entry with consultation summary
    }
    
    @EventListener
    public void onLabResultUploaded(LabResultUploadedEvent event) {
        // TODO: Create dossier entry with lab result
    }
    
    @EventListener
    public void onPreOpReadinessRequested(PreOpReadinessRequestedEvent event) {
        // TODO: Placeholder for Day 2
    }
    
    @EventListener
    public void onPostOpReportCreated(PostOpReportCreatedEvent event) {
        // TODO: Placeholder for Day 2
    }
    
    @EventListener
    public void onDischargeCreated(DischargeCreatedEvent event) {
        // TODO: Placeholder for Day 2
    }
}
```

**Fix appointment 15-min auto-cancel rule**:
- Find appointment cancellation logic in clinical-service (search for "auto-cancel" or "20 min" or "30 min")
- Replace with exactly 15 minutes
- Add comment: "HAS requirement: auto-cancel after 15 minutes inactivity"

**✅ Checkpoint**: Dossier entity persists, event listeners compile, appointment rule fixed.

---

## DAY 2: Entities + Business Rules + Alerts (7 hours)

### 2.1 ConsultationRecord Entity (1.5 hours)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/entity/ConsultationRecord.java`

**Implement with embedded components**:
- **Core fields**: patientId, consultationType, admissionMode, referringPhysicianId, consultationDate, attendingPhysicianId, chiefComplaint
- **@Embeddable Vitals**: weight_kg, height_cm, headCircumference_cm, bpSystolic, bpDiastolic, bpMeasurementLimb (enum: RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG), bpCuffSize (enum: NEONATAL, INFANT, CHILD, SMALL_ADULT, ADULT), heartRate_bpm, respiratoryRate_bpm, temperature_C, oxygenSaturation_pct, edemasPresent, edemasLocation, nutritionalAssessment
- **@Embeddable SOAP**: subjectiveSOAP, objectiveSOAP, assessmentSOAP, planSOAP
- **@Embedded PediatricNephrologyRecord**: for nephrology-specific findings
- **@ElementCollection allergies**: List<AllergyEntry> (with AllergyType, responsibleAgent, reactionType, severity, status)
- **@OneToMany diagnoses**: ICD-10 code, label, primary/secondary flag

---

### 2.2 HospitalizationRecord Entity (1.5 hours)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/entity/HospitalizationRecord.java`

**Implement with @Embeddable sections**:
- **@Embeddable NeonatalData**: gestationalAgeAtBirth_weeks, birthWeight_g, birthLength_cm, birthHeadCircumference_cm, hypotrophy, apgarScore1min/5min/10min, pregnancyType, deliveryMode, deliveryInduced, birthResuscitation (list of enums)
- **@Embeddable RespiratorySection** (gate: hasRespiratoryPathology Boolean): respiratoryPathologyType, surfactantAdministered, bpdSeverity, ventilatorySupportAt28d, ventilatorySupportAt36wks
- **@Embeddable CardiacSection** (gate: hasCardiacPathology): cardiacPathologies (list), pdaTreatment
- **@Embeddable NeurologicalSection**: intraventricularHemorrhage (IVHGrade), periventricularLeukomalacia (enum), seizures (enum), neurologyCodingScore
- **@Embeddable InfectiousSection**: maternalFetalInfection, infectiousAgents (list), multiResistantBacteria (HAS mandatory), lateInfection
- **@Embeddable AuditoryVisionSection**: hearingScreeningStatus, hearingResult, hearingCodingScore, ropStage, ropTreatment, visionCodingScore
- **@Embedded PediatricNephrologyRecord**

---

### 2.3 DischargeDocument Entity (1 hour)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/entity/DischargeDocument.java`

**5 MANDATORY HAS sections**:
- `admissionReason: String` (required)
- `medicalSummary: String` (required)
- `@OneToMany technicalActs: List<TechnicalAct>` (required)
- `@OneToMany medicationsAtDischarge: List<MedicationAtDischarge>` (required)
- `@Embedded followUpPlan: FollowUpPlan` (required)

**Medico-administrative**:
- `dischargeDate, dischargeDestination, dischargeWeight_g, crhDocumentStatus, guardianConsentForDMP, redactorId, redactionDate, distributionList, documentValidAsCRH`

---

### 2.4 ClinicalValidationService (2 hours)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/service/ClinicalValidationService.java`

**Implement 7 business rules**:
1. Schwartz validation: height + creatinine required (use validator annotation or @PrePersist)
2. CKD stage auto-compute: inject SchwartzGFRCalculator, compute on @PrePersist
3. minimalChangeLikely auto-flag: if age 1-10y && hematuria=ABSENT && edema=true && complement_C3 normal && eGFR normal
4. HAS discharge validation: throw HASComplianceException if sections 1,2,3,4,5 missing
5. CRH 8-day alert: if status=PARTIAL_PENDING_8_DAYS, schedule alert 7 days post-discharge
6. HUS annual follow-up: if husPresent && husOutcome!=FATAL, create annual follow-up task
7. MedicationAtDischarge justification: if status=MODIFIED/STOPPED, modificationJustification required

---

### 2.5 ClinicalAlertsService (1 hour)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/service/ClinicalAlertsService.java`

**Implement 7 alerts** + create `ClinicalAlert` entity:
8. vigilanceAlert escalation: if true, notify team
9. Nephrotoxic drug alert: scan list, flag each drug
10. BP threshold auto-classify: compute HTASeverity from BP + age (Battisti formula)
11. Protein intake validation: if eGFR < 10 && proteinIntake > 1.2, raise alert
12. Phosphate alert: if serumPhosphate > 1.5, alert
13. Calcium urgent alert: if serumCalcium < 1.75, urgent alert
14. Potassium urgent alert: if serumPotassium > 6.0, urgent alert

---

### 2.6 HASDischargeValidator Component (0.5 hour)

Create: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/validator/HASDischargeValidator.java`

**Gate before DischargeDocument.finalize()**: validate sections 1,2,3,4,5 non-empty

---

### 2.7 Event Wiring (1 hour)

Complete `MedicalDossierEventListener` from Day 1:
- ConsultationClosed → dossier entry
- LabResultUploaded → dossier entry
- DischargeCreated → dossier entry
- **Publish** PrescriptionCreated event (pharmacy-service listens)

---

## DAY 3: DTOs + REST API + Tests (7 hours)

### 3.1 Request/Response DTOs (1.5 hours)

Create in `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/dto/`:
- `ConsultationRecordRequest` + `ConsultationRecordResponse` (with computed: ckdStage, htaSeverity, activeAlerts)
- `HospitalizationRecordRequest` + `HospitalizationRecordResponse`
- `PediatricNephrologyRecordRequest` + `PediatricNephrologyRecordResponse`
- `DischargeDocumentRequest` + `DischargeDocumentResponse`
- `MedicalDossierEntryResponse`

Add validation: `@NotNull`, `@Positive`, `@Min`, `@Max` where applicable

---

### 3.2 MapStruct Mappers (1 hour)

Create in `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/mapper/`:
- Mappers for all 5 entities
- Ensure mappers call `SchwartzGFRCalculator` and `ClinicalAlertsService` to populate computed fields

---

### 3.3 REST Endpoints (2 hours)

Create controller: `BackEnd/microservices/clinical-service/src/main/java/com/nephropaidi/clinical/controller/ClinicalController.java`

```
POST   /api/v1/consultations                           → create + emit ConsultationClosed
GET    /api/v1/consultations/{id}                      → retrieve
POST   /api/v1/hospitalizations                        → create
GET    /api/v1/hospitalizations/{id}                   → retrieve
POST   /api/v1/discharge-documents                     → create
GET    /api/v1/discharge-documents/{id}                → retrieve
POST   /api/v1/discharge-documents/{id}/finalize       → gate with HASDischargeValidator
GET    /api/v1/patients/{patientId}/gfr                → latest Schwartz eGFR
GET    /api/v1/patients/{patientId}/alerts             → active clinical alerts
GET    /api/v1/patients/{patientId}/medical-dossier    → timeline
GET    /api/v1/patients/{patientId}/medical-dossier?filters=consultation,lab,discharge → filtered
```

---

### 3.4 Integration Tests (2.5 hours)

Create: `BackEnd/microservices/clinical-service/src/test/java/com/nephropaidi/clinical/integration/`

**8+ tests**:
1. Schwartz neonate (k=0.45)
2. Schwartz child (k=0.55)
3. Schwartz adolescent (k=0.70)
4. Save consultation → eGFR auto-computed → ckdStage assigned → dossier entry created
5. Save discharge missing section 4 → HASDischargeValidator rejects
6. Save discharge complete → persists with status=COMPLETE
7. Save hospitalization with Ca2+ < 1.75 → urgent alert created
8. Save consultation with K+ > 6.0 → urgent alert created

---

## COMPLETION CHECKLIST

- [ ] Day 1: All 65 enums compiling
- [ ] Day 1: Schwartz calculator tested (3 age groups)
- [ ] Day 1: PediatricNephrologyRecord skeleton + dossier model
- [ ] Day 1: Appointment 15-min rule fixed
- [ ] Day 2: ConsultationRecord, HospitalizationRecord, DischargeDocument entities
- [ ] Day 2: 14 business rules enforced
- [ ] Day 2: HASDischargeValidator working
- [ ] Day 2: Event listeners wired
- [ ] Day 3: DTOs with computed fields
- [ ] Day 3: MapStruct mappers
- [ ] Day 3: 11 REST endpoints responding
- [ ] Day 3: 8+ integration tests passing
- [ ] Day 3: Swagger documentation generated

---

## NOTES FOR SUCCESS

✅ **Critical path**: You are blocking Person B & C until Day 2 afternoon (dossier ready)  
✅ **Focus first**: Schwartz calculator is the most complex; test thoroughly  
✅ **Use @Embeddable wisely**: Keep ConsultationRecord lean; embed vitals, SOAP, nephrology data  
✅ **HAS compliance is non-negotiable**: 5 mandatory sections must be present before finalization  
✅ **Document k-values clearly**: ai-clinical-service will copy your logic on Day 1 EOD

---

**Start Day 1 at 09:00. You've got this! 🚀**
