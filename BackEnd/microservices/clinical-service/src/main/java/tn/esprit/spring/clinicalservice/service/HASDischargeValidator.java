package tn.esprit.spring.clinicalservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;
import tn.esprit.spring.clinicalservice.repository.MedicationAtDischargeRepository;
import tn.esprit.spring.clinicalservice.repository.TechnicalActRepository;

/**
 * HAS (Haute Autorité de Santé) discharge validator.
 * Enforces compliance with 5 mandatory discharge document sections.
 * Throws HASComplianceException if validation fails.
 */
@Slf4j
@Component
public class HASDischargeValidator {
    
    @Autowired
    private TechnicalActRepository technicalActRepository;
    
    @Autowired
    private MedicationAtDischargeRepository medicationRepository;
    
    /**
     * Validate discharge document for HAS compliance
     * @param discharge document to validate
     * @throws HASComplianceException if any section is incomplete
     */
    public void validate(DischargeDocument discharge) throws HASComplianceException {
        
        // Section 1: Admission reason (required)
        if (discharge.getAdmissionReason() == null || discharge.getAdmissionReason().trim().isEmpty()) {
            throw new HASComplianceException("HAS Compliance Error: Section 1 - Admission reason is required");
        }
        
        // Section 2: Medical summary (required)
        if (discharge.getMedicalSummary() == null || discharge.getMedicalSummary().trim().isEmpty()) {
            throw new HASComplianceException("HAS Compliance Error: Section 2 - Medical summary is required");
        }
        
        // Section 3: Technical acts (required - at least one act)
        if (discharge.getId() != null) {
            var technicalActs = technicalActRepository.findByDischargeId(discharge.getId());
            if (technicalActs == null || technicalActs.isEmpty()) {
                throw new HASComplianceException("HAS Compliance Error: Section 3 - At least one technical act is required");
            }
        }
        
        // Section 4: Medications at discharge (required - at least one medication)
        if (discharge.getId() != null) {
            var medications = medicationRepository.findByDischargeId(discharge.getId());
            if (medications == null || medications.isEmpty()) {
                throw new HASComplianceException("HAS Compliance Error: Section 4 - At least one medication is required");
            }
        }
        
        // Section 5: Follow-up plan (required)
        if (discharge.getFollowUpPlan() == null) {
            throw new HASComplianceException("HAS Compliance Error: Section 5 - Follow-up plan is required");
        }
        if (discharge.getFollowUpPlan().getFollowupObjectives() == null || 
            discharge.getFollowUpPlan().getFollowupObjectives().trim().isEmpty()) {
            throw new HASComplianceException("HAS Compliance Error: Section 5 - Follow-up objectives are required");
        }
        
        log.info("HAS validation successful for discharge document {}", discharge.getId());
    }
    
    /**
     * Custom exception for HAS compliance violations
     */
    public static class HASComplianceException extends Exception {
        public HASComplianceException(String message) {
            super(message);
        }
        
        public HASComplianceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
