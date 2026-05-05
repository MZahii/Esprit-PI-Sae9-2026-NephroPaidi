package tn.esprit.spring.opsservice.hospitalization.api.dto;

import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HospitalizationCaseResponse(
        UUID id,
        Long patientId,
        UUID consultationId,
        String doctorKeycloakId,
        String doctorUsername,
        String reason,
        HospitalizationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<HospitalizationTaskResponse> tasks
) {
}
