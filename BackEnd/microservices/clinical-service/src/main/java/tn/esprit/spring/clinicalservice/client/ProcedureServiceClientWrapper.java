package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/**
 * Wrapper for ProcedureServiceClient with explicit circuit breaker fallback methods
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcedureServiceClientWrapper {

    private final ProcedureServiceClient procedureServiceClient;

    @CircuitBreaker(name = "procedure-service", fallbackMethod = "getSurgicalCaseFallback")
    public ResponseEntity<Object> getSurgicalCase(UUID id, String token) {
        log.debug("Fetching surgical case {} from procedure-service", id);
        return procedureServiceClient.getSurgicalCase(id, token);
    }

    public ResponseEntity<Object> getSurgicalCaseFallback(UUID id, String token, Exception ex) {
        log.warn("Procedure-service unavailable for getSurgicalCase({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("procedure-service unavailable"));
    }

    @CircuitBreaker(name = "procedure-service", fallbackMethod = "getAllSurgicalCasesFallback")
    public ResponseEntity<List<Object>> getAllSurgicalCases(String token) {
        log.debug("Fetching all surgical cases from procedure-service");
        return procedureServiceClient.getAllSurgicalCases(token);
    }

    public ResponseEntity<List<Object>> getAllSurgicalCasesFallback(String token, Exception ex) {
        log.warn("Procedure-service unavailable for getAllSurgicalCases. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "procedure-service", fallbackMethod = "getDialysisSessionFallback")
    public ResponseEntity<Object> getDialysisSession(UUID id, String token) {
        log.debug("Fetching dialysis session {} from procedure-service", id);
        return procedureServiceClient.getDialysisSession(id, token);
    }

    public ResponseEntity<Object> getDialysisSessionFallback(UUID id, String token, Exception ex) {
        log.warn("Procedure-service unavailable for getDialysisSession({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("procedure-service unavailable"));
    }

    @CircuitBreaker(name = "procedure-service", fallbackMethod = "getAllDialysisSessionsFallback")
    public ResponseEntity<List<Object>> getAllDialysisSessions(String token) {
        log.debug("Fetching all dialysis sessions from procedure-service");
        return procedureServiceClient.getAllDialysisSessions(token);
    }

    public ResponseEntity<List<Object>> getAllDialysisSessionsFallback(String token, Exception ex) {
        log.warn("Procedure-service unavailable for getAllDialysisSessions. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "procedure-service", fallbackMethod = "getDialysisPlanFallback")
    public ResponseEntity<Object> getDialysisPlan(Long id, String token) {
        log.debug("Fetching dialysis plan {} from procedure-service", id);
        return procedureServiceClient.getDialysisPlan(id, token);
    }

    public ResponseEntity<Object> getDialysisPlanFallback(Long id, String token, Exception ex) {
        log.warn("Procedure-service unavailable for getDialysisPlan({}). Using fallback.", id, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("procedure-service unavailable"));
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
