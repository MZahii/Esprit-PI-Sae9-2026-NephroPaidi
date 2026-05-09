package tn.esprit.spring.clinicalservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.spring.clinicalservice.dto.AIPredictionRequestDTO;
import tn.esprit.spring.clinicalservice.dto.AIPredictionResponseDTO;

/**
 * Fallback implementation for AIClinicalServiceClient
 * Used when AI service is unavailable to prevent cascade failures
 */
@Slf4j
@Component
public class AIClinicalServiceClientFallback implements AIClinicalServiceClient {
    
    @Override
    public AIPredictionResponseDTO predict(AIPredictionRequestDTO request) {
        log.warn("AI Clinical Service unavailable. Returning default 'review' recommendation.");
        
        // Return conservative recommendation ("review") when AI service is down
        return AIPredictionResponseDTO.builder()
            .prediction("review")
            .confidence(0.0)
            .classProbabilities(java.util.Map.of(
                "other", 0.25,
                "review", 0.50,
                "urgent", 0.15,
                "warning", 0.10
            ))
            .build();
    }
}
