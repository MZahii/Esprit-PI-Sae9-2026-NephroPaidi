package tn.esprit.spring.opsservice.hospitalization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationMeasurementKind;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskType;

public record CreateHospitalizationTaskRequest(
        @NotNull HospitalizationTaskType type,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1000) String instructions,
        HospitalizationMeasurementKind measurementKind,
        @Size(max = 32) String expectedUnit,
        Integer displayOrder
) {
}
