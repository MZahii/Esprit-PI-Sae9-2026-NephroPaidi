package tn.esprit.spring.mlpredictionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PredictionRequest(
        @NotBlank String phase,
        @NotNull Long surgicalCaseId,
        @NotBlank String patientId,
        String consultationId,
        Integer age,
        String gender,
        String urgencyLevel,
        String surgeryType,
        String procedureName,
        String bloodType,
        String allergies,
        String noteText,
        Integer complicationCount,
        Integer openCareTaskCount,
        Integer linkedLabRequestCount,
        Map<String, String> features
) {
}
