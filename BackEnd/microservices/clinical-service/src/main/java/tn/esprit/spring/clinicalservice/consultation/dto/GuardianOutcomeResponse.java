package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GuardianOutcomeResponse {
    private UUID consultationId;
    private String prescriptions;
    private String treatmentPlan;
    private LocalDateTime updatedAt;
}
