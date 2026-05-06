package tn.esprit.spring.procedureservice.surgical.dto.ml;

import java.util.Map;

public record MlPredictionResponse(
        String phase,
        String modelName,
        String modelVersion,
        String predictionLabel,
        String riskLevel,
        double probability,
        String recommendation,
        Map<String, Double> explanations
) {
}
