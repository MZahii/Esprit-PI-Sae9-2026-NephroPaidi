package tn.esprit.spring.clinicalservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for HospitalizationRecord REST endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalizationRecordDTO {
    private UUID id;

    @NotNull
    private UUID patientId;

    @NotNull
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private String admissionReason;
    
    // Neonatal fields
    @Positive
    private Integer birthWeight_g;
    private Integer gestationalAgeAtBirth_weeks;
    private BigDecimal birthLength_cm;
    private BigDecimal birthHeadCircumference_cm;
    private String hypotrophy;
    private Integer apgarScore1min;
    private Integer apgarScore5min;
    private String pregnancyType;
    private String deliveryMode;
    private Boolean deliveryInduced;
    
    // Respiratory section
    private Boolean hasRespiratoryPathology;
    private String respiratoryPathologyType;
    private String surfactantAdministered;
    private String bpdSeverity;
    private Boolean ventilatorySupportAt28d;
    private Boolean ventilatorySupportAt36wks;
    
    // Cardiac section
    private Boolean hasCardiacPathology;
    private String cardiacPathologyType;
    private String pdaTreatment;
    
    // Neurological section
    private String intraventricularHemorrhage;
    private Boolean periventricularLeukomalacia;
    private Boolean seizures;
    private String neurologyCodingScore;

    // Infectious section
    private String maternalFetalInfection;
    private Boolean multiResistantBacteria;
    private Boolean lateInfection;

    // Auditory/vision section
    private Boolean hearingScreeningStatus;
    private String hearingResult;
    private String hearingCodingScore;
    private String ropStage;
    private String ropTreatment;
    private String visionCodingScore;
    
    // Nephrology embedded
    private BigDecimal eGFR;
    private String ckdStage;
    private String ckdCause;
    private BigDecimal serumCreatinine_mgdL;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
