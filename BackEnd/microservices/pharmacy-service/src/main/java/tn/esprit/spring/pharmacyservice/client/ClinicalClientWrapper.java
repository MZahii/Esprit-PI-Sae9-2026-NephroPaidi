package tn.esprit.spring.pharmacyservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Wrapper for ClinicalClient with explicit circuit breaker fallback methods
 * Provides safe communication with clinical-service and graceful degradation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalClientWrapper {

    private final ClinicalClient clinicalClient;

    @CircuitBreaker(name = "clinical-service", fallbackMethod = "getConsultationFallback")
    public ResponseEntity<Object> getConsultation(String id, String token) {
        log.debug("Fetching consultation {} from clinical-service", id);
        return clinicalClient.getConsultation(id, token);
    }

    public ResponseEntity<Object> getConsultationFallback(String id, String token, Exception ex) {
        log.warn("Clinical-service unavailable for getConsultation({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("clinical-service unavailable for getConsultation"));
    }

    @CircuitBreaker(name = "clinical-service", fallbackMethod = "getAppointmentFallback")
    public ResponseEntity<Object> getAppointment(String id, String token) {
        log.debug("Fetching appointment {} from clinical-service", id);
        return clinicalClient.getAppointment(id, token);
    }

    public ResponseEntity<Object> getAppointmentFallback(String id, String token, Exception ex) {
        log.warn("Clinical-service unavailable for getAppointment({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("clinical-service unavailable for getAppointment"));
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
