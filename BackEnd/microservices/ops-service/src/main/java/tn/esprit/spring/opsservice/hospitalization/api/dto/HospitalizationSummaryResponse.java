package tn.esprit.spring.opsservice.hospitalization.api.dto;

import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalizationSummaryResponse(
        UUID id,
        Long patientId,
        UUID consultationId,
        String doctorUsername,
        String reason,
        HospitalizationStatus status,
        int totalTasks,
        int completedTasks,
        int pendingTasks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
