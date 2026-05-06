package tn.esprit.spring.procedureservice.surgical.dto.ml;

import java.util.Map;

public record MlPredictionRequest(
        String phase,
        Long surgicalCaseId,
        String patientId,
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
