package tn.esprit.spring.communicationservice.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AiTriageClient {

    private final AiTriageProperties properties;

    public AiTriageResult triage(AiTriageRequest request) {
        if (!properties.isEnabled()) {
            return AiTriageResult.skipped("AI triage is disabled");
        }

        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        if (baseUrl == null) {
            return AiTriageResult.skipped("AI triage base URL is not configured");
        }

        Instant evaluatedAt = Instant.now();
        try {
            AiTriageResponse response = restClient(baseUrl)
                    .post()
                    .uri(URI.create(baseUrl + "/predict"))
                    .body(request)
                    .retrieve()
                    .body(AiTriageResponse.class);

            if (response == null) {
                return AiTriageResult.failed("AI triage returned an empty response");
            }

            return AiTriageResult.builder()
                    .urgencyLevel(response.getUrgencyLevel())
                    .confidence(response.getConfidence())
                    .status(response.getStatus())
                    .explanation(response.getExplanation())
                    .modelVersion(response.getModelVersion())
                    .evaluatedAt(evaluatedAt)
                    .build();
        } catch (RestClientException | IllegalArgumentException ex) {
            return AiTriageResult.failed("AI triage request failed: " + ex.getClass().getSimpleName());
        }
    }

    private RestClient restClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(properties.getTimeoutMillis(), 1);
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMillis));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("/+$", "");
    }
}
