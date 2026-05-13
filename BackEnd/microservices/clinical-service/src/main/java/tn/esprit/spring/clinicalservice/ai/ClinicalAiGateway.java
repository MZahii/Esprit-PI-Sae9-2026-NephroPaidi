package tn.esprit.spring.clinicalservice.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tn.esprit.spring.clinicalservice.ai.dto.AiLabAnalysisRequest;
import tn.esprit.spring.clinicalservice.ai.dto.AiLabAnalysisResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicalAiGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.ai-clinical.base-url:http://core-ops-service:8086/api/ai/biomarker-extraction}")
    private String aiBaseUrl;

    public AiLabAnalysisResponse analyzeLabResult(AiLabAnalysisRequest request) {
        RestClient client = restClientBuilder.baseUrl(aiBaseUrl).build();
        log.info("Sending lab result to AI service at {}", aiBaseUrl);
        return client.post()
                .uri("/infer-lab-result")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiLabAnalysisResponse.class);
    }
}
