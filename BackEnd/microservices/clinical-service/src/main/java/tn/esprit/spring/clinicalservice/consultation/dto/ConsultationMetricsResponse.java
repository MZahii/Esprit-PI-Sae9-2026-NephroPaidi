package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation Metrics Response DTO
 * 
 * Contains:
 * - Patient biometric data (height, weight, age)
 * - Serum creatinine (both mg/dL and µmol/L for flexibility)
 * - CKD-EPI calculated eGFR (mL/min/1.73m²)
 * - CKD stage classification and clinical alerts
 * - Trend analysis data (previous eGFR, change %, trend status)
 * - Quality scoring and calculation method used
 */
@Getter
@Builder
public class ConsultationMetricsResponse {
    private UUID id;
    private UUID consultationId;
    private Long patientId;
    
    // Biometric data
    private Double heightCm;
    private Double weightKg;
    private Integer ageYears;
    
    // Serum creatinine (both units)
    private Double creatinineMgDl;
    private Double creatinineUmol;  // SI units (µmol/L)
    private String serumCreatinineUnit;
    
    // eGFR calculation results
    private Double egfr;
    private Double ckdEpiEgfr;
    private String egfrFormulaUsed;  // "CKD_EPI_2021" or "COCKCROFT_GAULT"
    private String egfrQualityIndicator;  // "HIGH_QUALITY", "MEDIUM_QUALITY", "LOW_QUALITY"
    
    // Trend analysis
    private Double previousEgfr;
    private Double egfrChange;
    private Double egfrChangePercent;
    private String egfrTrend;
    private LocalDateTime egfrLastUpdatedAt;
    
    // CKD stage and alerts
    private String ckdStage;
    private Boolean alertLowEgfr;
    private Boolean alertRapidDecline;
    private String alertMessage;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
