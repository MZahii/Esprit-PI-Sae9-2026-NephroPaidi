package tn.esprit.spring.clinicalservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for ConsultationRecord REST endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRecordDTO {
    private UUID id;

    @NotNull
    private UUID patientId;

    @NotBlank
    private String consultationType;

    @NotBlank
    private String admissionMode;

    @NotNull
    private LocalDateTime consultationDate;
    private String chiefComplaint;
    
    // VitalSigns embedded fields
    @Positive
    private BigDecimal weight_kg;

    @Positive
    private BigDecimal height_cm;

    @Min(30)
    @Max(300)
    private Integer bpSystolic_mmHg;

    @Min(20)
    @Max(200)
    private Integer bpDiastolic_mmHg;
    private Integer heartRate_bpm;
    private Integer respiratoryRate_bpm;
    private BigDecimal temperature_C;

    @Min(0)
    @Max(100)
    private Integer oxygenSaturation_pct;
    
    // PediatricNephrology embedded fields
    private BigDecimal eGFR;
    private String ckdStage;
    private String ckdCause;
    private BigDecimal serumCreatinine_mgdL;
    private String proteinuriaCategory;
    private String hematuria;
    private BigDecimal serumAlbumin_g_L;
    private BigDecimal serumPotassium_mmolL;
    
    // Allergies
    private List<AllergyEntryDTO> allergies;
    
    // AI Document Metadata (for PDF/Scanner parsing)
    private Float parserConfidence;      // 0.0-1.0 confidence from document scanner
    private String contentType;           // "application/pdf", "image/jpeg", etc.
    private Boolean requiresManualReview; // Flag if document parsing was uncertain
    private Integer documentAgeDays;      // Days since document was created
    private Boolean isReviewed;           // Clinical staff has reviewed this consultation
    private Integer hospitalId;           // Hospital identifier (0 = unknown)
    private Integer departmentId;         // Department identifier (0 = unknown)
    private BigDecimal serumCreatinine_umolL; // Creatinine in μmol/L (for AI model)
    private Float documentQualityScore;   // Calculated quality score (0-100)

    // Optional clinical computation inputs
    private Integer ageYears;
    private Boolean isPremature;
    private BigDecimal proteinIntakeGPerKgPerDay;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
