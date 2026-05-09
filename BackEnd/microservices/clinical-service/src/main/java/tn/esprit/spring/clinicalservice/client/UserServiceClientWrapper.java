package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Wrapper for UserServiceClient with explicit circuit breaker fallback methods
 * Provides safe inter-service communication with graceful degradation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClientWrapper {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    public ResponseEntity<Object> getUserById(String userId, String token) {
        log.debug("Fetching user {} from user-service", userId);
        return userServiceClient.getUserById(userId, token);
    }

    public ResponseEntity<Object> getUserByIdFallback(String userId, String token, Exception ex) {
        log.warn("User-service unavailable for getUserById({}). Using fallback.", userId, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("user-service unavailable for getUserById"));
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "searchStaffFallback")
    public ResponseEntity<Object> searchStaff(Object request, String token) {
        log.debug("Searching staff in user-service");
        return userServiceClient.searchStaff(request, token);
    }

    public ResponseEntity<Object> searchStaffFallback(Object request, String token, Exception ex) {
        log.warn("User-service unavailable for searchStaff. Using fallback.", ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("user-service unavailable for searchStaff"));
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getAllUsersFallback")
    public ResponseEntity<List<Object>> getAllUsers(String token) {
        log.debug("Fetching all users from user-service");
        return userServiceClient.getAllUsers(token);
    }

    public ResponseEntity<List<Object>> getAllUsersFallback(String token, Exception ex) {
        log.warn("User-service unavailable for getAllUsers. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getGuardiansFallback")
    public ResponseEntity<List<Object>> getGuardians(String token) {
        log.debug("Fetching guardians from user-service");
        return userServiceClient.getGuardians(token);
    }

    public ResponseEntity<List<Object>> getGuardiansFallback(String token, Exception ex) {
        log.warn("User-service unavailable for getGuardians. Using fallback.", ex);
        return ResponseEntity.ok(List.of());
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserAuditLogsFallback")
    public ResponseEntity<Object> getUserAuditLogs(String userId, String token) {
        log.debug("Fetching audit logs for user {} from user-service", userId);
        return userServiceClient.getUserAuditLogs(userId, token);
    }

    public ResponseEntity<Object> getUserAuditLogsFallback(String userId, String token, Exception ex) {
        log.warn("User-service unavailable for getUserAuditLogs({}). Using fallback.", userId, ex);
        return ResponseEntity.status(503)
                .body(createErrorResponse("user-service unavailable for getUserAuditLogs"));
    }

    private Object createErrorResponse(String message) {
        return new ErrorResponse(message, "service_unavailable");
    }

    record ErrorResponse(String message, String error) {}
}
