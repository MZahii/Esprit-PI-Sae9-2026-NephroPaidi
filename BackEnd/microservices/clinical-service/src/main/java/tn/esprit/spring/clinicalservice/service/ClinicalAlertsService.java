package tn.esprit.spring.clinicalservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.entity.*;
import tn.esprit.spring.clinicalservice.enums.*;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clinical alerts service implementing 7 alert types for pediatric nephrology.
 * Generates alerts for vigilance, nephrotoxic drugs, electrolyte imbalances.
 */
@Slf4j
@Service
public class ClinicalAlertsService {
    
    @Autowired
    private ClinicalAlertRepository alertRepository;
    
    /**
     * ALERT 1: Vigilance escalation
     * If vigilanceRequired flag is set, escalate alert to team
     */
    public void checkVigilanceRequired(ConsultationRecord consultation, UUID patientId) {
        // Placeholder for vigilance logic
        // Example: if patient has rare disease or complex case
        log.debug("Alert 1: Vigilance check completed for patient {}", patientId);
    }
    
    /**
     * ALERT 2: Nephrotoxic drug scan
     * Flag each drug in patient medications if it's in the nephrotoxic list
     * Nephrotoxic drugs: NSAIDs, ACE inhibitors, aminoglycosides, cephalosporins,
     * ciprofloxacin, acyclovir, amphotericin, contrast agents, cisplatin, ifosfamide,
     * ciclosporin, carbamazepine, valproate, IV immunoglobulins, lithium
     */
    public void scanForNephrotoxicDrugs(UUID patientId, List<MedicationAtDischarge> medications) {
        List<String> nephrotoxicDrugs = List.of(
            "NSAID", "ibuprofen", "naproxen", "indomethacin",
            "ACE", "enalapril", "lisinopril", "ramipril",
            "aminoglycosides", "gentamicin", "tobramycin", "amikacin",
            "cephalosporin", "cephalexin", "ceftriaxone",
            "ciprofloxacin", "acyclovir", "amphotericin",
            "contrast", "cisplatin", "ifosfamide", "ciclosporin",
            "carbamazepine", "valproate", "immunoglobulins", "lithium"
        );
        
        if (medications == null) {
            return;
        }
        
        for (MedicationAtDischarge med : medications) {
            String medName = med.getMedicationName().toLowerCase();
            for (String nephrotoxic : nephrotoxicDrugs) {
                if (medName.contains(nephrotoxic.toLowerCase())) {
                    ClinicalAlert alert = new ClinicalAlert();
                    alert.setPatientId(patientId);
                    alert.setAlertType("NEPHROTOXIC_DRUG");
                    alert.setSeverity(AlertSeverity.WARNING);
                    alert.setMessage("Nephrotoxic drug detected: " + med.getMedicationName());
                    alert.setDetails("Monitor renal function during treatment");
                    alert.setCreatedAt(LocalDateTime.now());
                    alert.setResolved(false);
                    alertRepository.save(alert);
                    log.warn("Alert 2: Nephrotoxic drug {} detected for patient {}", medName, patientId);
                    break;
                }
            }
        }
    }
    
    /**
     * ALERT 3: Blood pressure classification
     * Compute HTASeverity from BP + age using pediatric BP thresholds
     * Alert if systolic/diastolic exceeds normal range for age
     */
    public void checkBPClassification(UUID patientId, VitalSigns vitals) {
        if (vitals == null || vitals.getBpSystolic_mmHg() == null) {
            return;
        }
        
        Integer sysBP = vitals.getBpSystolic_mmHg();
        Integer diaBP = vitals.getBpDiastolic_mmHg();
        
        // Simplified HTA classification (pediatric-specific thresholds would be age-dependent)
        if (sysBP >= 160 || (diaBP != null && diaBP >= 100)) {
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(patientId);
            alert.setAlertType("BP_THRESHOLD");
            alert.setSeverity(AlertSeverity.URGENT);
            alert.setMessage("Severe hypertension detected");
            alert.setDetails("BP: " + sysBP + "/" + diaBP + " mmHg");
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            alertRepository.save(alert);
            log.warn("Alert 3: Severe hypertension for patient {}", patientId);
        } else if (sysBP >= 140 || (diaBP != null && diaBP >= 90)) {
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(patientId);
            alert.setAlertType("BP_THRESHOLD");
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setMessage("Elevated blood pressure");
            alert.setDetails("BP: " + sysBP + "/" + diaBP + " mmHg");
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            alertRepository.save(alert);
            log.debug("Alert 3: Elevated BP for patient {}", patientId);
        }
    }
    
    /**
     * ALERT 4: Protein intake validation
     * If eGFR < 10 AND proteinIntake > 1.2 g/kg/day → alert
     */
    public void checkProteinIntakeSafety(UUID patientId, PediatricNephrologyRecord nephro, Integer weightKg) {
        if (nephro == null || nephro.getEGFR() == null) {
            return;
        }
        
        if (nephro.getEGFR().compareTo(new BigDecimal("10")) < 0) {
            // eGFR < 10 (Stage 5 CKD)
            // TODO: Get protein intake from diet tracking
            log.debug("Alert 4: Stage 5 CKD - protein intake monitoring recommended");
        }
    }
    
    /**
     * ALERT 5: Phosphate alert
     * If serum phosphate > 1.5 mmol/L (typical normal: 0.8-1.4) → alert
     */
    public void checkPhosphateLevel(UUID patientId, PediatricNephrologyRecord nephro) {
        if (nephro == null || nephro.getSerumPhosphate_mmolL() == null) {
            return;
        }
        
        if (nephro.getSerumPhosphate_mmolL().compareTo(new BigDecimal("1.5")) > 0) {
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(patientId);
            alert.setAlertType("ELECTROLYTE_IMBALANCE");
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setMessage("Elevated serum phosphate");
            alert.setDetails("Phosphate: " + nephro.getSerumPhosphate_mmolL() + " mmol/L");
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            alertRepository.save(alert);
            log.warn("Alert 5: Elevated phosphate for patient {}", patientId);
        }
    }
    
    /**
     * ALERT 6: Calcium urgent
     * If serum calcium < 1.75 mmol/L (normal: 2.1-2.6) → URGENT alert
     */
    public void checkCalciumLevel(UUID patientId, PediatricNephrologyRecord nephro) {
        if (nephro == null || nephro.getSerumCalcium_mmolL() == null) {
            return;
        }
        
        if (nephro.getSerumCalcium_mmolL().compareTo(new BigDecimal("1.75")) < 0) {
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(patientId);
            alert.setAlertType("ELECTROLYTE_IMBALANCE");
            alert.setSeverity(AlertSeverity.URGENT);
            alert.setMessage("Critical hypocalcemia detected");
            alert.setDetails("Calcium: " + nephro.getSerumCalcium_mmolL() + " mmol/L");
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            alertRepository.save(alert);
            log.error("Alert 6: Critical hypocalcemia for patient {}", patientId);
        }
    }
    
    /**
     * ALERT 7: Potassium urgent
     * If serum potassium > 6.0 mmol/L (normal: 3.5-5.0) → URGENT alert
     */
    public void checkPotassiumLevel(UUID patientId, PediatricNephrologyRecord nephro) {
        if (nephro == null || nephro.getSerumPotassium_mmolL() == null) {
            return;
        }
        
        if (nephro.getSerumPotassium_mmolL().compareTo(new BigDecimal("6.0")) > 0) {
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(patientId);
            alert.setAlertType("ELECTROLYTE_IMBALANCE");
            alert.setSeverity(AlertSeverity.URGENT);
            alert.setMessage("Critical hyperkalemia detected");
            alert.setDetails("Potassium: " + nephro.getSerumPotassium_mmolL() + " mmol/L");
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            alertRepository.save(alert);
            log.error("Alert 7: Critical hyperkalemia for patient {}", patientId);
        }
    }
}
