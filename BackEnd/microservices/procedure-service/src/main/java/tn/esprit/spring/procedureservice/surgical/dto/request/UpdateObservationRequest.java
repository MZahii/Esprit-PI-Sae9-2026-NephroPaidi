package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateObservationRequest(@NotBlank String notes) {
}
