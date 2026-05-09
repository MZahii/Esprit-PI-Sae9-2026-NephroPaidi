package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign client for Pharmacy-Service communication
 * Provides methods to manage medication orders and check availability
 */
@FeignClient(
    name = "pharmacy-service",
    url = "${services.pharmacy.url:http://localhost:8087}",
    configuration = FeignClientConfiguration.class
)
public interface PharmacyClient {
    
    @PostMapping("/api/medications/orders")
    ResponseEntity<Object> createMedicationOrder(
        @RequestBody Object request,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/medications/{id}")
    ResponseEntity<Object> getMedicationById(
        @PathVariable String id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/medications")
    ResponseEntity<List<Object>> getAllMedications(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/medications/batches/expired")
    ResponseEntity<List<Object>> getExpiredBatches(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @PostMapping("/api/medications/{id}/orders/validate")
    ResponseEntity<Boolean> validateOrderAvailability(
        @PathVariable String id,
        @RequestParam int quantity,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}

/**
 * Fallback implementation for Pharmacy client circuit breaker
 * Handles graceful degradation when pharmacy-service is unavailable
 */
@Component
@Slf4j
class PharmacyClientFallback implements PharmacyClient {
    
    @Override
    public ResponseEntity<Object> createMedicationOrder(Object request, String token) {
        log.warn("Pharmacy service unavailable - falling back from createMedicationOrder");
        return ResponseEntity.status(503).body(null);
    }
    
    @Override
    public ResponseEntity<Object> getMedicationById(String id, String token) {
        log.warn("Pharmacy service unavailable - cannot get medication {}", id);
        return ResponseEntity.status(503).body(null);
    }
    
    @Override
    public ResponseEntity<List<Object>> getAllMedications(String token) {
        log.warn("Pharmacy service unavailable - returning empty medications list");
        return ResponseEntity.ok(List.of());
    }
    
    @Override
    public ResponseEntity<List<Object>> getExpiredBatches(String token) {
        log.warn("Pharmacy service unavailable - returning empty batches list");
        return ResponseEntity.ok(List.of());
    }
    
    @Override
    public ResponseEntity<Boolean> validateOrderAvailability(String id, int quantity, String token) {
        log.warn("Pharmacy service unavailable - cannot validate availability");
        return ResponseEntity.status(503).body(false);
    }
}
