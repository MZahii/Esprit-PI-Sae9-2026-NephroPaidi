package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationOutcomeResponse {
    private UUID consultationId;
    private String notes;
    private String diagnosis;
    private String prescriptions;
    private String labRequests;
    private String treatmentPlan;
    private LocalDateTime updatedAt;
}
