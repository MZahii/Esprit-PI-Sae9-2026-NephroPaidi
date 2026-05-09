package tn.esprit.spring.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalNotificationBridgeService {

    @Value("${administration-service.base-url:http://localhost:8087}")
    private String administrationServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void pushNotification(String type, String title, String message, Long targetUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Key", internalApiKey);

            Map<String, Object> payload = Map.of(
                    "type", type,
                    "title", title,
                    "message", message,
                    "targetUserId", targetUserId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(
                    administrationServiceBaseUrl + "/api/observability/internal/notifications",
                    entity,
                    Void.class
            );
        } catch (Exception ex) {
            log.warn("Failed to push notification to administration-service: {}", ex.getMessage());
        }
    }
}
