package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.spring.clinicalservice.dto.AIPredictionRequestDTO;
import tn.esprit.spring.clinicalservice.dto.AIPredictionResponseDTO;

/**
 * Feign client for communication with AI Clinical Service
 * Handles prediction requests for clinical alert recommendations
 */
@FeignClient(
    name = "ai-clinical-service",
    url = "${services.ai-clinical.base-url:http://ai-clinical-service:5000}",
    fallback = AIClinicalServiceClientFallback.class
)
public interface AIClinicalServiceClient {
    
    /**
     * Request AI prediction for clinical lab data
     * @param request AIPredictionRequestDTO with extracted lab values
     * @return AIPredictionResponseDTO with predicted class and confidence
     */
    @PostMapping("/predict")
    AIPredictionResponseDTO predict(@RequestBody AIPredictionRequestDTO request);
}
