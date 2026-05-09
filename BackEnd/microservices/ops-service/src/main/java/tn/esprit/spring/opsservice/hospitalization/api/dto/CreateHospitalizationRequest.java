package tn.esprit.spring.opsservice.hospitalization.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CreateHospitalizationRequest(
        @NotNull Long patientId,
        UUID consultationId,
        @NotBlank @Size(max = 1000) String reason,
        @Valid List<CreateHospitalizationTaskRequest> tasks
) {
    public List<CreateHospitalizationTaskRequest> safeTasks() {
        return tasks == null ? new ArrayList<>() : tasks;
    }
}
