package tn.esprit.spring.procedureservice.dialysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDialysisPrescriptionRequest(@NotNull Long planId, @NotBlank String details) {
}
