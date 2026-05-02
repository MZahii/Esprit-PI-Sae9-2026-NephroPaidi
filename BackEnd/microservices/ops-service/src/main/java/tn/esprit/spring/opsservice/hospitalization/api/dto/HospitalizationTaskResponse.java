package tn.esprit.spring.opsservice.hospitalization.api.dto;

import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationMeasurementKind;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HospitalizationTaskResponse(
        UUID id,
        HospitalizationTaskType type,
        String title,
        String instructions,
        HospitalizationMeasurementKind measurementKind,
        String expectedUnit,
        Integer displayOrder,
        HospitalizationTaskStatus status,
        String latestNote,
        BigDecimal latestNumericValue,
        String latestTextValue,
        String latestUnit,
        String lastUpdatedByNurseId,
        String lastUpdatedByNurseUsername,
        LocalDateTime lastUpdatedAt,
        List<HospitalizationTaskExecutionResponse> executions
) {
}
