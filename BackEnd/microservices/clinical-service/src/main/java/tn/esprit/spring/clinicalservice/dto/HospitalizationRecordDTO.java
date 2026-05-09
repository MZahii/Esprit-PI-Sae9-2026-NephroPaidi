package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
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
    private UUID patientId;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private String admissionReason;
    
    // Neonatal fields
    private Integer birthWeight_g;
    private Integer gestationalAgeAtBirth_weeks;
    private Integer apgarScore1min;
    private Integer apgarScore5min;
    
    // Respiratory section
    private Boolean hasRespiratoryPathology;
    private String respiratoryPathologyType;
    private String bpdSeverity;
    
    // Cardiac section
    private Boolean hasCardiacPathology;
    private String cardiacPathologyType;
    private String pdaTreatment;
    
    // Neurological section
    private String intraventricularHemorrhage;
    private Boolean seizures;
    
    // Nephrology embedded
    private String ckdStage;
    private String ckdCause;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
