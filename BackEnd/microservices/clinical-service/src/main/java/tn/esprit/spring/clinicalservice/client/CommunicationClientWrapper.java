package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Wrapper for CommunicationClient with explicit circuit breaker fallback methods
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommunicationClientWrapper {

    private final CommunicationClient communicationClient;

    @CircuitBreaker(name = "communication-service", fallbackMethod = "sendNotificationFallback")
    public ResponseEntity<Void> sendNotification(Object notification, String token) {
        log.debug("Sending notification to communication-service");
        return communicationClient.sendNotification(notification, token);
    }

    public ResponseEntity<Void> sendNotificationFallback(Object notification, String token, Exception ex) {
        log.warn("Communication-service unavailable for sendNotification. Using fallback.", ex);
        return ResponseEntity.status(503).build();
    }

    @CircuitBreaker(name = "communication-service", fallbackMethod = "createMessageFallback")
    public ResponseEntity<Object> createMessage(Object messageRequest, String token) {
        log.debug("Creating message in communication-service");
        return communicationClient.createMessage(messageRequest, token);
    }

    public ResponseEntity<Object> createMessageFallback(Object messageRequest, String token, Exception ex) {
        log.warn("Communication-service unavailable for createMessage. Using fallback.", ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("communication-service unavailable"));
    }

    @CircuitBreaker(name = "communication-service", fallbackMethod = "getMyAppointmentsFallback")
    public ResponseEntity<List<Object>> getMyAppointments(String token) {
        log.debug("Fetching appointments from communication-service");
        return communicationClient.getMyAppointments(token);
    }

    public ResponseEntity<List<Object>> getMyAppointmentsFallback(String token, Exception ex) {
        log.warn("Communication-service unavailable for getMyAppointments. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "communication-service", fallbackMethod = "createAppointmentRequestFallback")
    public ResponseEntity<Object> createAppointmentRequest(Object request, String token) {
        log.debug("Creating appointment request in communication-service");
        return communicationClient.createAppointmentRequest(request, token);
    }

    public ResponseEntity<Object> createAppointmentRequestFallback(Object request, String token, Exception ex) {
        log.warn("Communication-service unavailable for createAppointmentRequest. Using fallback.", ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("communication-service unavailable"));
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
