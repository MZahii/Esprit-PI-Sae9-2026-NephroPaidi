package tn.esprit.spring.opsservice.hospitalization.api.dto;

import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalizationTaskExecutionResponse(
        UUID id,
        HospitalizationTaskStatus status,
        String nurseKeycloakId,
        String nurseUsername,
        String note,
        BigDecimal numericValue,
        String textValue,
        String unit,
        LocalDateTime recordedAt
) {
}
