package tn.esprit.spring.procedureservice.dialysis.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDialysisPrescriptionRequest(@NotBlank String details) {
}
