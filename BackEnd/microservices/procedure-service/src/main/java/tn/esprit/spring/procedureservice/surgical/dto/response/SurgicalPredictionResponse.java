package tn.esprit.spring.procedureservice.surgical.dto.response;

import java.time.LocalDateTime;

public record SurgicalPredictionResponse(
        Long id,
        Long surgicalCaseId,
        String phase,
        String modelName,
        String modelVersion,
        String predictionLabel,
        String riskLevel,
        double probability,
        String recommendation,
        String inputSnapshotJson,
        String outputJson,
        LocalDateTime createdAt
) {
}
