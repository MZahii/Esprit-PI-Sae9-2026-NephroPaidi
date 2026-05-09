package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCareTaskRequest(@NotNull Long surgicalCaseId, @NotBlank String title) {
}
