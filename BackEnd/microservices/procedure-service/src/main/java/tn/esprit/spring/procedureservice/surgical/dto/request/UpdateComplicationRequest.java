package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateComplicationRequest(@NotBlank String description) {
}
