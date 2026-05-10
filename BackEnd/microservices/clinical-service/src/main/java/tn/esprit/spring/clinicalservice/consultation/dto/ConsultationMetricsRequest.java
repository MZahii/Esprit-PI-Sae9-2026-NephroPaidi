package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Consultation Metrics Request DTO
 * 
 * Required for CKD-EPI formula calculation:
 * - heightCm: Patient height in centimeters
 * - creatinineMgDl: Serum creatinine in mg/dL (will be converted to µmol/L)
 * - ageYears: Patient age in years
 * - sex: Patient gender ('M' or 'F') - required for CKD-EPI coefficients
 * - weightKg: Patient weight in kilograms (optional, for reference)
 */
@Getter
@Setter
public class ConsultationMetricsRequest {
    private Double heightCm;
    private Double creatinineMgDl;
    private Double weightKg;
    private Integer ageYears;
    private String sex;  // 'M' or 'F' - required for CKD-EPI formula
    private Double systolicBpMmHg;
    private Double diastolicBpMmHg;
    private Integer heartRateBpm;
    private Integer respiratoryRateBpm;
    private Double temperatureC;
    private Integer oxygenSaturationPct;
    private Double creatinineUmol;
    private String serumCreatinineUnit;
    private String egfrFormulaUsed;
    private Double ckdEpiEgfr;
    private Double egfr;
    private String ckdStage;
    private Double previousEgfr;
    private Double egfrChange;
    private Double egfrChangePercent;
    private String egfrTrend;
    private String egfrQualityIndicator;
    private String egfrLastUpdatedAt;
    private Boolean alertLowEgfr;
    private Boolean alertRapidDecline;
    private String alertMessage;
    private String aiRecommendation;
    private Double aiConfidence;
    private String aiSummary;
    private Boolean aiRequiresReview;
    private String aiSourceFileName;
    private String aiUpdatedAt;
}
