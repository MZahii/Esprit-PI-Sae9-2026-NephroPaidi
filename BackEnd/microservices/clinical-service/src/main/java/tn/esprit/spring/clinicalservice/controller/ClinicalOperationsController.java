package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.mapper.ConsultationRecordMapper;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;
import tn.esprit.spring.clinicalservice.service.ClinicalAlertsService;
import tn.esprit.spring.clinicalservice.service.ClinicalValidationService;
import java.util.UUID;

/**
 * REST Controller for Clinical Operations and Validation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clinical")
public class ClinicalOperationsController {
    
    @Autowired
    private ConsultationRecordRepository consultationRepository;
    
    @Autowired
    private ConsultationRecordMapper mapper;
    
    @Autowired
    private ClinicalValidationService validationService;
    
    @Autowired
    private ClinicalAlertsService alertsService;
    
    /**
     * POST /api/v1/clinical/consultations/{id}/compute-ckd
     * Compute CKD stage using Schwartz formula
     */
    @PostMapping("/consultations/{id}/compute-ckd")
    public ResponseEntity<?> computeCKDStage(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer ageYears,
            @RequestParam(required = false, defaultValue = "false") Boolean isPremature) {
        
        var optional = consultationRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ConsultationRecord consultation = optional.get();
        try {
            validationService.computeAndAssignCKDStage(consultation, ageYears, isPremature);
            ConsultationRecord saved = consultationRepository.save(consultation);
            log.info("CKD stage computed for consultation {}", id);
            return ResponseEntity.ok(mapper.toDTO(saved));
        } catch (Exception e) {
            log.error("Error computing CKD stage: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * POST /api/v1/clinical/consultations/{id}/validate
     * Validate consultation data (Schwartz requirements, etc)
     */
    @PostMapping("/consultations/{id}/validate")
    public ResponseEntity<?> validateConsultation(@PathVariable UUID id) {
        var optional = consultationRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ConsultationRecord consultation = optional.get();
        try {
            validationService.validateSchwartzRequirements(consultation.getNephologyRecord());
            log.info("Consultation {} validated successfully", id);
            return ResponseEntity.ok("Consultation validated successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for consultation {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * POST /api/v1/clinical/consultations/{id}/scan-alerts
     * Scan consultation for clinical alerts (BP, electrolytes, nephrotoxic drugs)
     */
    @PostMapping("/consultations/{id}/scan-alerts")
    public ResponseEntity<?> scanConsultationAlerts(@PathVariable UUID id) {
        var optional = consultationRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ConsultationRecord consultation = optional.get();
        try {
            // Scan for BP classification
            alertsService.checkBPClassification(consultation.getPatientId(), consultation.getVitalSigns(), null);
            
            // Scan for electrolyte levels
            alertsService.checkPhosphateLevel(consultation.getPatientId(), consultation.getNephologyRecord());
            alertsService.checkCalciumLevel(consultation.getPatientId(), consultation.getNephologyRecord());
            alertsService.checkPotassiumLevel(consultation.getPatientId(), consultation.getNephologyRecord());
            
            log.info("Alert scan completed for consultation {}", id);
            return ResponseEntity.ok("Alert scan completed");
        } catch (Exception e) {
            log.error("Error scanning alerts: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * POST /api/v1/clinical/consultations/{id}/check-minimal-change
     * Check if minimal change disease is likely (age 1-10, hematuria absent, edema present, normal eGFR)
     */
    @PostMapping("/consultations/{id}/check-minimal-change")
    public ResponseEntity<?> checkMinimalChangeLikelihood(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer ageYears) {
        
        var optional = consultationRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        ConsultationRecord consultation = optional.get();
        boolean isLikely = validationService.isMinimalChangeLikely(consultation, ageYears);
        if (isLikely) {
            log.info("Minimal change disease pattern detected for consultation {}", id);
            return ResponseEntity.ok("Minimal change disease likely - Pattern detected");
        } else {
            log.debug("Minimal change disease pattern NOT detected for consultation {}", id);
            return ResponseEntity.ok("Minimal change disease unlikely - Pattern not detected");
        }
    }
}
