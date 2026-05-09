package tn.esprit.spring.clinicalservice.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO for receiving prediction response from AI service
 * Contains the predicted class and confidence scores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPredictionResponseDTO {
    private String prediction;           // "other", "review", "urgent", "warning"
    private Double confidence;           // 0.0-1.0 confidence score
    
    @JsonProperty("class_probabilities")
    private Map<String, Double> classProbabilities; // Probabilities for each class
}
