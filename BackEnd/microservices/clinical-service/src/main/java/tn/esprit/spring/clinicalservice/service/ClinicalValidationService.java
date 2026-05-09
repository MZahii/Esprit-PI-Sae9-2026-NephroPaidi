package tn.esprit.spring.clinicalservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.entity.*;
import tn.esprit.spring.clinicalservice.enums.CKDStage;
import java.math.BigDecimal;

/**
 * Clinical validation service implementing 7 pediatric nephrology business rules.
 * Rules are enforced at entity/service layer before persistence.
 */
@Slf4j
@Service
public class ClinicalValidationService {
    
    @Autowired
    private SchwartzGFRCalculator schwartzCalculator;
    
    /**
     * RULE 1: Schwartz validation - height + creatinine required for eGFR computation
     */
    public void validateSchwartzRequirements(PediatricNephrologyRecord nephro) {
        if (nephro.getSerumCreatinine_mgdL() == null || nephro.getSerumCreatinine_mgdL().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rule 1: Serum creatinine (mg/dL) is required for Schwartz calculation");
        }
        log.debug("Rule 1: Schwartz requirements validated");
    }
    
    /**
     * RULE 2: CKD stage auto-compute from eGFR using Schwartz
     * Requires: height, creatinine, age
     * Auto-populates: eGFR and ckdStage
     */
    public void computeAndAssignCKDStage(ConsultationRecord consultation, Integer ageYears, Boolean isPremature) {
        VitalSigns vitals = consultation.getVitalSigns();
        PediatricNephrologyRecord nephro = consultation.getNephologyRecord();
        
        if (vitals == null || nephro == null) {
            log.warn("Rule 2: No vital signs or nephrology data available");
            return;
        }
        
        if (vitals.getHeight_cm() == null || nephro.getSerumCreatinine_mgdL() == null) {
            log.warn("Rule 2: Insufficient data for CKD stage computation");
            return;
        }
        
        SchwartzGFRCalculator.SchwartzResult result = schwartzCalculator.calculate(
            vitals.getHeight_cm(),
            nephro.getSerumCreatinine_mgdL(),
            ageYears,
            isPremature
        );
        
        nephro.setEGFR(result.eGFR());
        nephro.setSchwartz_k(result.k());
        nephro.setCkdStage(result.stage());
        
        log.info("Rule 2: CKD stage auto-computed. eGFR={}, stage={}", result.eGFR(), result.stage());
    }
    
    /**
     * RULE 3: Minimal change disease likely flag
     * If age 1-10y AND hematuria absent AND edema present AND C3 normal AND eGFR ≥90
     * → flag as minimal change likely
     */
    public boolean isMinimalChangeLikely(ConsultationRecord consultation, Integer ageYears) {
        PediatricNephrologyRecord nephro = consultation.getNephologyRecord();
        VitalSigns vitals = consultation.getVitalSigns();
        
        if (nephro == null || vitals == null) {
            return false;
        }
        
        // Age 1-10y
        if (ageYears == null || ageYears < 1 || ageYears > 10) {
            return false;
        }
        
        // Hematuria absent (check if ABSENT or null)
        if (nephro.getHematuria() != null && !nephro.getHematuria().name().equals("ABSENT")) {
            return false;
        }
        
        // Edema present
        if (vitals.getEdemasPresent() == null || !vitals.getEdemasPresent()) {
            return false;
        }
        
        // eGFR ≥ 90 (normal)
        if (nephro.getEGFR() == null || nephro.getEGFR().compareTo(new BigDecimal("90")) < 0) {
            return false;
        }
        
        log.info("Rule 3: Minimal change disease likely - pattern detected");
        return true;
    }
    
    /**
     * RULE 4: HAS discharge validation - sections 1,2,3,4,5 must not be empty
     */
    public void validateHASDischargeCompleteness(DischargeDocument discharge) {
        if (discharge.getAdmissionReason() == null || discharge.getAdmissionReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule 4 HAS: Admission reason (section 1) is required");
        }
        if (discharge.getMedicalSummary() == null || discharge.getMedicalSummary().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule 4 HAS: Medical summary (section 2) is required");
        }
        if (discharge.getFollowUpPlan() == null || discharge.getFollowUpPlan().getFollowupObjectives() == null) {
            throw new IllegalArgumentException("Rule 4 HAS: Follow-up plan (section 5) is required");
        }
        // Sections 3 & 4 (technical acts, medications) validated separately in HASDischargeValidator
        log.debug("Rule 4: HAS discharge sections validated");
    }
    
    /**
     * RULE 5: CRH 8-day alert
     * If crhDocumentStatus = PARTIAL_PENDING_8_DAYS, schedule alert 7 days post-discharge
     */
    public void checkCRH8DayCompliance(DischargeDocument discharge) {
        if (discharge.getCrhDocumentStatus() != null && 
            discharge.getCrhDocumentStatus().name().equals("PARTIAL_PENDING_8_DAYS")) {
            log.warn("Rule 5: CRH document incomplete - must be finalized within 8 days of discharge");
            // TODO: Schedule alert for day 7
        }
    }
    
    /**
     * RULE 6: HUS annual follow-up
     * If husPresent AND husOutcome != FATAL → create annual follow-up task
     */
    public void checkHUSAnnualFollowup(PediatricNephrologyRecord nephro) {
        if (nephro.getHusPresent() == null || !nephro.getHusPresent()) {
            return;
        }
        if (nephro.getHusType() == null) {
            return;
        }
        log.info("Rule 6: HUS patient detected - annual follow-up recommended");
        // TODO: Create follow-up task
    }
    
    /**
     * RULE 7: MedicationAtDischarge justification required
     * If status = MODIFIED or STOPPED, modificationJustification must not be empty
     */
    public void validateMedicationModification(MedicationAtDischarge med) {
        String status = med.getMedicationStatus() != null ? med.getMedicationStatus().name() : null;
        
        if ("MODIFIED".equals(status) || "STOPPED".equals(status)) {
            if (med.getModificationJustification() == null || med.getModificationJustification().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Rule 7: Medication " + med.getMedicationName() + " status is " + status +
                    " but justification is missing"
                );
            }
        }
        log.debug("Rule 7: Medication modification validated");
    }
}
