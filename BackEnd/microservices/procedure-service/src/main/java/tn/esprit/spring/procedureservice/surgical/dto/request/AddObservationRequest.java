package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddObservationRequest(@NotNull Long surgicalCaseId, @NotBlank String notes) {
}
