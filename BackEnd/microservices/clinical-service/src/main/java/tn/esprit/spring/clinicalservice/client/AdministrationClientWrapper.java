package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Wrapper for AdministrationClient with explicit circuit breaker fallback methods
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdministrationClientWrapper {

    private final AdministrationClient administrationClient;

    @CircuitBreaker(name = "administration-service", fallbackMethod = "getPatientFallback")
    public ResponseEntity<Object> getPatient(String patientId, String token) {
        log.debug("Fetching patient {} from administration-service", patientId);
        return administrationClient.getPatient(patientId, token);
    }

    public ResponseEntity<Object> getPatientFallback(String patientId, String token, Exception ex) {
        log.warn("Administration-service unavailable for getPatient({}). Using fallback.", patientId, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("administration-service unavailable"));
    }

    @CircuitBreaker(name = "administration-service", fallbackMethod = "getAllPatientsFallback")
    public ResponseEntity<List<Object>> getAllPatients(String token) {
        log.debug("Fetching all patients from administration-service");
        return administrationClient.getAllPatients(token);
    }

    public ResponseEntity<List<Object>> getAllPatientsFallback(String token, Exception ex) {
        log.warn("Administration-service unavailable for getAllPatients. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "administration-service", fallbackMethod = "getContractFallback")
    public ResponseEntity<Object> getContract(String contractId, String token) {
        log.debug("Fetching contract {} from administration-service", contractId);
        return administrationClient.getContract(contractId, token);
    }

    public ResponseEntity<Object> getContractFallback(String contractId, String token, Exception ex) {
        log.warn("Administration-service unavailable for getContract({}). Using fallback.", contractId, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("administration-service unavailable"));
    }

    @CircuitBreaker(name = "administration-service", fallbackMethod = "getContractsByStaffFallback")
    public ResponseEntity<List<Object>> getContractsByStaff(String staffUserId, String token) {
        log.debug("Fetching contracts for staff {} from administration-service", staffUserId);
        return administrationClient.getContractsByStaff(staffUserId, token);
    }

    public ResponseEntity<List<Object>> getContractsByStaffFallback(String staffUserId, String token, Exception ex) {
        log.warn("Administration-service unavailable for getContractsByStaff({}). Using fallback.", staffUserId, ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "administration-service", fallbackMethod = "getPatientsByGuardianFallback")
    public ResponseEntity<List<Object>> getPatientsByGuardian(String guardianId, String token) {
        log.debug("Fetching patients for guardian {} from administration-service", guardianId);
        return administrationClient.getPatientsByGuardian(guardianId, token);
    }

    public ResponseEntity<List<Object>> getPatientsByGuardianFallback(String guardianId, String token, Exception ex) {
        log.warn("Administration-service unavailable for getPatientsByGuardian({}). Using fallback.", guardianId, ex);
        return ResponseEntity.ok(List.of());
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
