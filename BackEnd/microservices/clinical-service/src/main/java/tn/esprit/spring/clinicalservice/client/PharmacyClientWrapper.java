package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Wrapper for PharmacyClient with explicit circuit breaker fallback methods
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PharmacyClientWrapper {

    private final PharmacyClient pharmacyClient;

    @CircuitBreaker(name = "pharmacy-service", fallbackMethod = "createMedicationOrderFallback")
    public ResponseEntity<Object> createMedicationOrder(Object request, String token) {
        log.debug("Creating medication order in pharmacy-service");
        return pharmacyClient.createMedicationOrder(request, token);
    }

    public ResponseEntity<Object> createMedicationOrderFallback(Object request, String token, Exception ex) {
        log.warn("Pharmacy-service unavailable for createMedicationOrder. Using fallback.", ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("pharmacy-service unavailable"));
    }

    @CircuitBreaker(name = "pharmacy-service", fallbackMethod = "getMedicationByIdFallback")
    public ResponseEntity<Object> getMedicationById(String id, String token) {
        log.debug("Fetching medication {} from pharmacy-service", id);
        return pharmacyClient.getMedicationById(id, token);
    }

    public ResponseEntity<Object> getMedicationByIdFallback(String id, String token, Exception ex) {
        log.warn("Pharmacy-service unavailable for getMedicationById({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("pharmacy-service unavailable"));
    }

    @CircuitBreaker(name = "pharmacy-service", fallbackMethod = "getAllMedicationsFallback")
    public ResponseEntity<List<Object>> getAllMedications(String token) {
        log.debug("Fetching all medications from pharmacy-service");
        return pharmacyClient.getAllMedications(token);
    }

    public ResponseEntity<List<Object>> getAllMedicationsFallback(String token, Exception ex) {
        log.warn("Pharmacy-service unavailable for getAllMedications. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "pharmacy-service", fallbackMethod = "getExpiredBatchesFallback")
    public ResponseEntity<List<Object>> getExpiredBatches(String token) {
        log.debug("Fetching expired batches from pharmacy-service");
        return pharmacyClient.getExpiredBatches(token);
    }

    public ResponseEntity<List<Object>> getExpiredBatchesFallback(String token, Exception ex) {
        log.warn("Pharmacy-service unavailable for getExpiredBatches. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "pharmacy-service", fallbackMethod = "validateOrderAvailabilityFallback")
    public ResponseEntity<Boolean> validateOrderAvailability(String id, int quantity, String token) {
        log.debug("Validating order availability for medication {} (qty: {})", id, quantity);
        return pharmacyClient.validateOrderAvailability(id, quantity, token);
    }

    public ResponseEntity<Boolean> validateOrderAvailabilityFallback(String id, int quantity, String token, Exception ex) {
        log.warn("Pharmacy-service unavailable for validateOrderAvailability({}). Using fallback - assuming unavailable.", id, ex);
        return ResponseEntity.ok(false);
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
