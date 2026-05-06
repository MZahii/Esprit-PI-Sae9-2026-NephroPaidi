package tn.esprit.spring.mlpredictionservice.dto;

import java.util.Map;

public record PredictionResponse(
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
