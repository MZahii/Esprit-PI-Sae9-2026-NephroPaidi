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
}
