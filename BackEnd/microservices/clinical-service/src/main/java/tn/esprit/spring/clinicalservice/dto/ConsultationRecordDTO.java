package tn.esprit.spring.clinicalservice.dto;

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
    private UUID patientId;
    private String consultationType;
    private String admissionMode;
    private LocalDateTime consultationDate;
    private String chiefComplaint;
    
    // VitalSigns embedded fields
    private BigDecimal weight_kg;
    private BigDecimal height_cm;
    private Integer bpSystolic_mmHg;
    private Integer bpDiastolic_mmHg;
    private Integer heartRate_bpm;
    private Integer respiratoryRate_bpm;
    private BigDecimal temperature_C;
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
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
