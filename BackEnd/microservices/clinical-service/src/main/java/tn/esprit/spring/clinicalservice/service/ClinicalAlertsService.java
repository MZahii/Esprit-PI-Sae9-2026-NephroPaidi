package tn.esprit.spring.clinicalservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.entity.*;
import tn.esprit.spring.clinicalservice.enums.*;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Clinical alerts service implementing 7 alert types for pediatric nephrology.
 * Generates alerts for vigilance, nephrotoxic drugs, electrolyte imbalances.
 */
@Slf4j
@Service
public class ClinicalAlertsService {

    private static final Map<Integer, Integer> SBP_P90_BY_AGE = Map.ofEntries(
        Map.entry(2, 104), Map.entry(3, 106), Map.entry(4, 108), Map.entry(5, 109),
        Map.entry(6, 111), Map.entry(7, 113), Map.entry(8, 115), Map.entry(9, 117),
        Map.entry(10, 119), Map.entry(11, 121), Map.entry(12, 123), Map.entry(13, 125),
        Map.entry(14, 127), Map.entry(15, 129), Map.entry(16, 131), Map.entry(17, 132),
        Map.entry(18, 133)
    );

    private static final Map<Integer, Integer> SBP_P95_BY_AGE = Map.ofEntries(
        Map.entry(2, 107), Map.entry(3, 109), Map.entry(4, 111), Map.entry(5, 113),
        Map.entry(6, 115), Map.entry(7, 117), Map.entry(8, 119), Map.entry(9, 121),
        Map.entry(10, 123), Map.entry(11, 125), Map.entry(12, 127), Map.entry(13, 129),
        Map.entry(14, 131), Map.entry(15, 133), Map.entry(16, 135), Map.entry(17, 136),
        Map.entry(18, 137)
    );

    private static final Map<Integer, Integer> DBP_P90_BY_AGE = Map.ofEntries(
        Map.entry(2, 63), Map.entry(3, 64), Map.entry(4, 66), Map.entry(5, 67),
        Map.entry(6, 68), Map.entry(7, 69), Map.entry(8, 70), Map.entry(9, 71),
        Map.entry(10, 72), Map.entry(11, 73), Map.entry(12, 74), Map.entry(13, 75),
        Map.entry(14, 76), Map.entry(15, 77), Map.entry(16, 78), Map.entry(17, 79),
        Map.entry(18, 80)
    );

    private static final Map<Integer, Integer> DBP_P95_BY_AGE = Map.ofEntries(
        Map.entry(2, 66), Map.entry(3, 67), Map.entry(4, 69), Map.entry(5, 70),
        Map.entry(6, 72), Map.entry(7, 73), Map.entry(8, 74), Map.entry(9, 75),
        Map.entry(10, 76), Map.entry(11, 77), Map.entry(12, 78), Map.entry(13, 79),
        Map.entry(14, 80), Map.entry(15, 81), Map.entry(16, 82), Map.entry(17, 83),
        Map.entry(18, 84)
    );
    
    @Autowired
    private ClinicalAlertRepository alertRepository;
    
    /**
     * ALERT 1: Vigilance escalation
     * If vigilanceRequired flag is set, escalate alert to team
     */
    public void checkVigilanceRequired(ConsultationRecord consultation, UUID patientId) {
        if (consultation == null) {
            return;
        }

        boolean requiresManualReview = Boolean.TRUE.equals(consultation.getRequiresManualReview());
        boolean lowConfidence = consultation.getParserConfidence() != null && consultation.getParserConfidence() < 0.70f;
        boolean advancedCkd = consultation.getNephologyRecord() != null
            && consultation.getNephologyRecord().getCkdStage() != null
            && consultation.getNephologyRecord().getCkdStage().ordinal() >= CKDStage.STAGE_4.ordinal();

        if (requiresManualReview || lowConfidence || advancedCkd) {
            createAlert(
                patientId,
                "VIGILANCE",
                advancedCkd ? AlertSeverity.URGENT : AlertSeverity.WARNING,
                "Clinical vigilance escalation required",
                buildVigilanceDetails(requiresManualReview, lowConfidence, advancedCkd)
            );
        }
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
        checkBPClassification(patientId, vitals, null);
    }

    public void checkBPClassification(UUID patientId, VitalSigns vitals, Integer ageYears) {
        if (vitals == null || vitals.getBpSystolic_mmHg() == null) {
            return;
        }
        
        Integer sysBP = vitals.getBpSystolic_mmHg();
        Integer diaBP = vitals.getBpDiastolic_mmHg();

        if (ageYears != null && ageYears >= 2 && ageYears <= 18) {
            int sbp90 = SBP_P90_BY_AGE.get(ageYears);
            int sbp95 = SBP_P95_BY_AGE.get(ageYears);
            int dbp90 = DBP_P90_BY_AGE.get(ageYears);
            int dbp95 = DBP_P95_BY_AGE.get(ageYears);

            boolean severe = sysBP >= sbp95 + 12 || (diaBP != null && diaBP >= dbp95 + 12);
            boolean warning = sysBP >= sbp95 || (diaBP != null && diaBP >= dbp95);
            boolean routine = sysBP >= sbp90 || (diaBP != null && diaBP >= dbp90);

            if (severe) {
                createAlert(patientId, "BP_THRESHOLD", AlertSeverity.URGENT,
                    "Severe pediatric hypertension detected",
                    "Age " + ageYears + "y BP " + sysBP + "/" + diaBP + " mmHg exceeds >=P95+12 threshold");
            } else if (warning) {
                createAlert(patientId, "BP_THRESHOLD", AlertSeverity.WARNING,
                    "Pediatric blood pressure above 95th percentile",
                    "Age " + ageYears + "y BP " + sysBP + "/" + diaBP + " mmHg exceeds >=P95 threshold");
            } else if (routine) {
                createAlert(patientId, "BP_THRESHOLD", AlertSeverity.ROUTINE,
                    "Pediatric blood pressure above 90th percentile",
                    "Age " + ageYears + "y BP " + sysBP + "/" + diaBP + " mmHg exceeds >=P90 threshold");
            }
            return;
        }

        if (sysBP >= 160 || (diaBP != null && diaBP >= 100)) {
            createAlert(patientId, "BP_THRESHOLD", AlertSeverity.URGENT,
                "Severe hypertension detected", "BP: " + sysBP + "/" + diaBP + " mmHg");
        } else if (sysBP >= 140 || (diaBP != null && diaBP >= 90)) {
            createAlert(patientId, "BP_THRESHOLD", AlertSeverity.WARNING,
                "Elevated blood pressure", "BP: " + sysBP + "/" + diaBP + " mmHg");
        }
    }
    
    /**
     * ALERT 4: Protein intake validation
     * If eGFR < 10 AND proteinIntake > 1.2 g/kg/day → alert
     */
    public void checkProteinIntakeSafety(UUID patientId, PediatricNephrologyRecord nephro, BigDecimal proteinIntakeGPerKgPerDay) {
        if (nephro == null || nephro.getEGFR() == null) {
            return;
        }
        
        if (nephro.getEGFR().compareTo(new BigDecimal("10")) < 0 && proteinIntakeGPerKgPerDay != null
            && proteinIntakeGPerKgPerDay.compareTo(new BigDecimal("1.2")) > 0) {
            createAlert(
                patientId,
                "PROTEIN_INTAKE",
                AlertSeverity.WARNING,
                "Protein intake exceeds safety threshold for severe CKD",
                "eGFR " + nephro.getEGFR() + " with protein intake " + proteinIntakeGPerKgPerDay + " g/kg/day"
            );
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
            createAlert(patientId, "ELECTROLYTE_IMBALANCE", AlertSeverity.WARNING,
                "Elevated serum phosphate", "Phosphate: " + nephro.getSerumPhosphate_mmolL() + " mmol/L");
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
            createAlert(patientId, "ELECTROLYTE_IMBALANCE", AlertSeverity.URGENT,
                "Critical hypocalcemia detected", "Calcium: " + nephro.getSerumCalcium_mmolL() + " mmol/L");
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
            createAlert(patientId, "ELECTROLYTE_IMBALANCE", AlertSeverity.URGENT,
                "Critical hyperkalemia detected", "Potassium: " + nephro.getSerumPotassium_mmolL() + " mmol/L");
            log.error("Alert 7: Critical hyperkalemia for patient {}", patientId);
        }
    }

    private void createAlert(UUID patientId, String alertType, AlertSeverity severity, String message, String details) {
        ClinicalAlert alert = new ClinicalAlert();
        alert.setPatientId(patientId);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setDetails(details);
        alert.setCreatedAt(LocalDateTime.now());
        alert.setResolved(false);
        alertRepository.save(alert);
    }

    private String buildVigilanceDetails(boolean requiresManualReview, boolean lowConfidence, boolean advancedCkd) {
        StringBuilder details = new StringBuilder();
        if (requiresManualReview) {
            details.append("Manual document review flagged. ");
        }
        if (lowConfidence) {
            details.append("Low parser confidence detected. ");
        }
        if (advancedCkd) {
            details.append("Advanced CKD stage requires close monitoring.");
        }
        return details.toString().trim();
    }
}
