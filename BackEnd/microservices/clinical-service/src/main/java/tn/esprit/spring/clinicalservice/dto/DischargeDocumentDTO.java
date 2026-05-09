package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for DischargeDocument REST endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DischargeDocumentDTO {
    private UUID id;
    private UUID patientId;
    private LocalDateTime dischargeDate;
    
    // HAS Sections
    private String admissionReason;
    private String medicalSummary;
    private List<TechnicalActDTO> technicalActs;
    private List<MedicationAtDischargeDTO> medications;
    private FollowUpPlanDTO followUpPlan;
    
    // Medico-administrative
    private String dischargeDestination;
    private Integer dischargeWeight_g;
    private String crhDocumentStatus;
    private Boolean guardianConsentForDMP;
    private String distributionList;
    private Boolean documentValidAsCRH;
    private Boolean finalized;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finalizedAt;
}
