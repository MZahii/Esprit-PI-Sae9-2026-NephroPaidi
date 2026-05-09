package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign client for Communication-Service communication
 * Provides methods to send notifications and manage messages
 */
@FeignClient(
    name = "communication-service",
    url = "${services.communication.url:http://localhost:8089}",
    configuration = FeignClientConfiguration.class
)
public interface CommunicationClient {
    
    @PostMapping("/api/communication/notifications")
    ResponseEntity<Void> sendNotification(
        @RequestBody Object notification,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @PostMapping("/api/communication/messages")
    ResponseEntity<Object> createMessage(
        @RequestBody Object messageRequest,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/appointments/my")
    ResponseEntity<List<Object>> getMyAppointments(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @PostMapping("/api/appointments/requests")
    ResponseEntity<Object> createAppointmentRequest(
        @RequestBody Object request,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}

/**
 * Fallback implementation for Communication client circuit breaker
 * Handles graceful degradation when communication-service is unavailable
 */
@Component
@Slf4j
class CommunicationClientFallback implements CommunicationClient {
    
    @Override
    public ResponseEntity<Void> sendNotification(Object notification, String token) {
        log.error("Communication service down - notification not sent");
        return ResponseEntity.status(503).build();
    }
    
    @Override
    public ResponseEntity<Object> createMessage(Object messageRequest, String token) {
        log.error("Communication service down - message not created");
        return ResponseEntity.status(503).build();
    }
    
    @Override
    public ResponseEntity<List<Object>> getMyAppointments(String token) {
        log.warn("Communication service down - returning empty appointments");
        return ResponseEntity.ok(List.of());
    }
    
    @Override
    public ResponseEntity<Object> createAppointmentRequest(Object request, String token) {
        log.error("Communication service down - appointment request not created");
        return ResponseEntity.status(503).build();
    }
}
