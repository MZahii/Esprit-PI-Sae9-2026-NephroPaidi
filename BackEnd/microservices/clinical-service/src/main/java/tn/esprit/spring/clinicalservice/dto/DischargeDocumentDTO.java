package tn.esprit.spring.clinicalservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private UUID patientId;

    @NotNull
    private LocalDateTime dischargeDate;
    
    // HAS Sections
    @NotBlank
    private String admissionReason;

    @NotBlank
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
    private UUID redactorId;
    private LocalDateTime redactionDate;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finalizedAt;
}
