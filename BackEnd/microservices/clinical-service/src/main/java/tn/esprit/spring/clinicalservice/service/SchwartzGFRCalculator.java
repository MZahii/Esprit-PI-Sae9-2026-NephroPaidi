package tn.esprit.spring.clinicalservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.enums.CKDStage;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Schwartz formula eGFR calculator for pediatric patients.
 * Formula: eGFR = k × height_cm / creatinine_mg_dL
 * 
 * k-value selection (age-dependent):
 * - Premature: k = 0.33
 * - <1 year (term neonate): k = 0.45
 * - 1-13 years (child): k = 0.55
 * - ≥13 years (adolescent): k = 0.70
 */
@Slf4j
@Service
public class SchwartzGFRCalculator {
    
    /**
     * Calculate eGFR using Schwartz formula
     */
    public SchwartzResult calculate(BigDecimal heightCm, BigDecimal creatinineMgdL, 
                                    Integer ageYears, Boolean isPremature) {
        // Validate inputs
        if (heightCm == null || creatinineMgdL == null || ageYears == null) {
            throw new IllegalArgumentException("Height (cm), creatinine (mg/dL), and age (years) are required");
        }
        
        if (heightCm.compareTo(BigDecimal.ZERO) <= 0 || creatinineMgdL.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Height and creatinine must be > 0");
        }
        
        // Select k-value based on age and prematurity
        BigDecimal k = selectKValue(ageYears, isPremature);
        
        // Compute eGFR = k × height_cm / creatinine_mg_dL
        // Round to 1 decimal place
        BigDecimal eGFR = k.multiply(heightCm)
                          .divide(creatinineMgdL, 2, RoundingMode.HALF_UP);
        
        // Auto-compute CKD stage from eGFR
        CKDStage stage = computeStage(eGFR);
        
        log.info("Schwartz calculation: height={}cm, creatinine={}mg/dL, age={}y, isPremature={} -> k={}, eGFR={}, stage={}", 
                 heightCm, creatinineMgdL, ageYears, isPremature, k, eGFR, stage);
        
        return new SchwartzResult(eGFR, k, stage);
    }
    
    /**
     * Select k-value based on age and prematurity status
     */
    private BigDecimal selectKValue(Integer ageYears, Boolean isPremature) {
        if (isPremature != null && isPremature) {
            return new BigDecimal("0.33");  // premature
        } else if (ageYears < 1) {
            return new BigDecimal("0.45");  // term neonate
        } else if (ageYears < 13) {
            return new BigDecimal("0.55");  // child
        } else {
            return new BigDecimal("0.70");  // adolescent
        }
    }
    
    /**
     * Auto-compute CKD stage from eGFR value
     */
    private CKDStage computeStage(BigDecimal eGFR) {
        if (eGFR.compareTo(new BigDecimal("90")) >= 0) {
            return CKDStage.STAGE_1;
        } else if (eGFR.compareTo(new BigDecimal("60")) >= 0) {
            return CKDStage.STAGE_2;
        } else if (eGFR.compareTo(new BigDecimal("45")) >= 0) {
            return CKDStage.STAGE_3A;
        } else if (eGFR.compareTo(new BigDecimal("30")) >= 0) {
            return CKDStage.STAGE_3B;
        } else if (eGFR.compareTo(new BigDecimal("15")) >= 0) {
            return CKDStage.STAGE_4;
        } else {
            return CKDStage.STAGE_5;
        }
    }
    
    /**
     * Result record containing computed eGFR, k-value, and CKD stage
     */
    public record SchwartzResult(BigDecimal eGFR, BigDecimal k, CKDStage stage) {
        public String toDisplayString() {
            return String.format("eGFR=%s mL/min/1.73m², k=%s, CKD Stage=%s", 
                                eGFR, k, stage != null ? stage.name() : "UNKNOWN");
        }
    }
}
