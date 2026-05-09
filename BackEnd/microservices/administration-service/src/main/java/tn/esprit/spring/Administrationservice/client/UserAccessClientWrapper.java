package tn.esprit.spring.Administrationservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.esprit.spring.Administrationservice.exception.ExternalServiceException;

/**
 * Wrapper around UserAccessClient that adds circuit breaker resilience
 * and fallback behavior for inter-service communication with user-service.
 */
@Component
@RequiredArgsConstructor
public class UserAccessClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(UserAccessClientWrapper.class);
    private final UserAccessClient userAccessClient;
    private final String internalApiKey = "${internal.api-key}"; // Injected from config

    /**
     * Update user activation status with circuit breaker protection.
     * Falls back gracefully if user-service is unavailable.
     */
    @CircuitBreaker(name = "userAccessService", fallbackMethod = "updateActivationFallback")
    public UserResponse updateActivation(Long userId, boolean enabled, String apiKey, String actorUsername) {
        log.debug("Calling updateActivation for user: {}, enabled: {}", userId, enabled);
        return userAccessClient.updateActivation(userId, enabled, apiKey, actorUsername);
    }

    /**
     * Fallback for updateActivation when circuit breaker is open or service fails.
     * Returns a response marking update as pending.
     */
    public UserResponse updateActivationFallback(Long userId, boolean enabled, String apiKey, String actorUsername, Exception ex) {
        log.warn("User-service unavailable for activation update (userId: {}). Using fallback.", userId, ex);
        UserResponse fallback = new UserResponse();
        fallback.setId(userId);
        fallback.setActivationPending(true);
        return fallback;
    }

    /**
     * Update user status with circuit breaker protection.
     * Falls back gracefully if user-service is unavailable.
     */
    @CircuitBreaker(name = "userAccessService", fallbackMethod = "updateStatusFallback")
    public UserResponse updateStatus(Long userId, String status, String apiKey, String actorUsername) {
        log.debug("Calling updateStatus for user: {}, status: {}", userId, status);
        return userAccessClient.updateStatus(userId, status, apiKey, actorUsername);
    }

    /**
     * Fallback for updateStatus when circuit breaker is open or service fails.
     * Returns a response marking status update as pending.
     */
    public UserResponse updateStatusFallback(Long userId, String status, String apiKey, String actorUsername, Exception ex) {
        log.warn("User-service unavailable for status update (userId: {}). Using fallback.", userId, ex);
        UserResponse fallback = new UserResponse();
        fallback.setId(userId);
        fallback.setStatusUpdatePending(true);
        return fallback;
    }

    /**
     * Get user summary with circuit breaker protection.
     * Falls back gracefully if user-service is unavailable.
     */
    @CircuitBreaker(name = "userAccessService", fallbackMethod = "getUserSummaryFallback")
    public InternalUserSummary getUserSummary(Long userId, String apiKey) {
        log.debug("Calling getUserSummary for user: {}", userId);
        return userAccessClient.getUserSummary(userId, apiKey);
    }

    /**
     * Fallback for getUserSummary when circuit breaker is open or service fails.
     * Returns a summary marking user data as potentially stale.
     */
    public InternalUserSummary getUserSummaryFallback(Long userId,  String apiKey, Exception ex) {
        log.warn("User-service unavailable for user summary (userId: {}). Returning minimal fallback.", userId, ex);
        InternalUserSummary fallback = new InternalUserSummary();
        fallback.setId(userId);
        fallback.setActivationPending(true);
        return fallback;
    }

    /**
     * Get pending contract count with circuit breaker protection.
     * Falls back to 0 if user-service is unavailable.
     */
    @CircuitBreaker(name = "userAccessService", fallbackMethod = "getPendingContractCountOlderThanDaysFallback")
    public long getPendingContractCountOlderThanDays(int days, String apiKey) {
        log.debug("Calling getPendingContractCountOlderThanDays for period: {} days", days);
        return userAccessClient.getPendingContractCountOlderThanDays(days, apiKey);
    }

    /**
     * Fallback for getPendingContractCountOlderThanDays when circuit breaker is open or service fails.
     * Returns 0 to indicate no counts available.
     */
    public long getPendingContractCountOlderThanDaysFallback(int days, String apiKey, Exception ex) {
        log.warn("User-service unavailable for pending contract count. Returning 0 as fallback.", ex);
        return 0L; // Return 0 - safe fallback for counts
    }
}
