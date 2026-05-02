package tn.esprit.spring.opsservice.hospitalization.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskStatus;

import java.math.BigDecimal;

public record HospitalizationTaskUpdateRequest(
        @NotNull HospitalizationTaskStatus status,
        @Size(max = 2000) String note,
        BigDecimal numericValue,
        @Size(max = 255) String textValue,
        @Size(max = 32) String unit
) {
}
