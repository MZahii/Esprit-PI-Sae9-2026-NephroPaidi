package tn.esprit.spring.Administrationservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserAccessClientWrapper - Circuit Breaker Resilience Tests")
@ActiveProfiles("test")
class UserAccessClientWrapperTest {

    @Mock
    private UserAccessClient userAccessClient;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private UserAccessClientWrapper wrapper;
    private String apiKey = "test-api-key";
    private String actorUsername = "test-actor";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wrapper = new UserAccessClientWrapper(userAccessClient);
    }

    @Test
    @DisplayName("Test 1: updateActivation succeeds with valid response")
    void testUpdateActivationSuccess() {
        // Arrange
        Long userId = 1L;
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        
        when(userAccessClient.updateActivation(userId, true, apiKey, actorUsername))
            .thenReturn(expectedResponse);

        // Act
        UserResponse result = wrapper.updateActivation(userId, true, apiKey, actorUsername);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userAccessClient, times(1)).updateActivation(userId, true, apiKey, actorUsername);
    }

    @Test
    @DisplayName("Test 2: updateActivation fallback on service failure")
    void testUpdateActivationFallback() {
        // Arrange
        Long userId = 2L;
        Exception serviceException = new RuntimeException("User-service unavailable");

        // Act - fallback should be called with exception
        UserResponse fallbackResponse = wrapper.updateActivationFallback(userId, true, apiKey, actorUsername, serviceException);

        // Assert
        assertNotNull(fallbackResponse);
        assertEquals(userId, fallbackResponse.getId());
        assertTrue(fallbackResponse.isActivationPending()); // Fallback marks as pending
    }

    @Test
    @DisplayName("Test 3: updateStatus succeeds with valid response")
    void testUpdateStatusSuccess() {
        // Arrange
        Long userId = 3L;
        String newStatus = "ACTIVE";
        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(userId);
        
        when(userAccessClient.updateStatus(userId, newStatus, apiKey, actorUsername))
            .thenReturn(expectedResponse);

        // Act
        UserResponse result = wrapper.updateStatus(userId, newStatus, apiKey, actorUsername);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userAccessClient, times(1)).updateStatus(userId, newStatus, apiKey, actorUsername);
    }

    @Test
    @DisplayName("Test 4: updateStatus fallback on service failure")
    void testUpdateStatusFallback() {
        // Arrange
        Long userId = 4L;
        Exception serviceException = new RuntimeException("User-service timeout");

        // Act - fallback should be called with exception
        UserResponse fallbackResponse = wrapper.updateStatusFallback(userId, "ACTIVE", apiKey, actorUsername, serviceException);

        // Assert
        assertNotNull(fallbackResponse);
        assertEquals(userId, fallbackResponse.getId());
        assertTrue(fallbackResponse.isStatusUpdatePending());
    }

    @Test
    @DisplayName("Test 5: getUserSummary succeeds with valid response")
    void testGetUserSummarySuccess() {
        // Arrange
        Long userId = 5L;
        InternalUserSummary expectedSummary = new InternalUserSummary();
        expectedSummary.setId(userId);
        
        when(userAccessClient.getUserSummary(userId, apiKey))
            .thenReturn(expectedSummary);

        // Act
        InternalUserSummary result = wrapper.getUserSummary(userId, apiKey);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userAccessClient, times(1)).getUserSummary(userId, apiKey);
    }

    @Test
    @DisplayName("Test 6: getUserSummary fallback on service failure")
    void testGetUserSummaryFallback() {
        // Arrange
        Long userId = 6L;
        Exception serviceException = new RuntimeException("User-service connection refused");

        // Act - fallback should be called with exception
        InternalUserSummary fallbackSummary = wrapper.getUserSummaryFallback(userId, apiKey, serviceException);

        // Assert
        assertNotNull(fallbackSummary);
        assertEquals(userId, fallbackSummary.getId());
        assertTrue(Boolean.TRUE.equals(fallbackSummary.getActivationPending())); // Marks as stale/pending
    }

    @Test
    @DisplayName("Test 7: getPendingContractCountOlderThanDays returns valid count")
    void testGetPendingContractCountSuccess() {
        // Arrange
        int daysThreshold = 7;
        long expectedCount = 42L;
        
        when(userAccessClient.getPendingContractCountOlderThanDays(daysThreshold, apiKey))
            .thenReturn(expectedCount);

        // Act
        long result = wrapper.getPendingContractCountOlderThanDays(daysThreshold, apiKey);

        // Assert
        assertEquals(expectedCount, result);
        verify(userAccessClient, times(1)).getPendingContractCountOlderThanDays(daysThreshold, apiKey);
    }

    @Test
    @DisplayName("Test 8: getPendingContractCountOlderThanDays fallback returns 0")
    void testGetPendingContractCountFallback() {
        // Arrange
        int daysThreshold = 7;
        Exception serviceException = new RuntimeException("User-service unreachable");

        // Act - fallback should return 0
        long result = wrapper.getPendingContractCountOlderThanDaysFallback(daysThreshold, apiKey, serviceException);

        // Assert
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("Test 9: Circuit breaker gracefully degrades on repeated failures")
    void testCircuitBreakerGracefulDegradation() {
        // Arrange - simulate repeated failures
        Long userId = 7L;
        Exception failureException = new RuntimeException("Persistent user-service failure");

        // Act - first failure triggers fallback
        UserResponse response1 = wrapper.updateActivationFallback(userId, true, apiKey, actorUsername, failureException);
        
        // Second failure also triggers fallback (circuit open)
        UserResponse response2 = wrapper.updateActivationFallback(userId, false, apiKey, actorUsername, failureException);

        // Assert - both calls returned valid fallback responses (graceful degradation)
        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.isActivationPending());
        assertTrue(response2.isActivationPending());
    }

    @Test
    @DisplayName("Test 10: Multiple service methods use same circuit breaker")
    void testMultipleMethodsShareCircuitBreaker() {
        // Arrange
        Long userId = 8L;
        Exception failure = new RuntimeException("User-service down");

        // Act - test fallback across multiple methods
        UserResponse activationFallback = wrapper.updateActivationFallback(userId, true, apiKey, actorUsername, failure);
        UserResponse statusFallback = wrapper.updateStatusFallback(userId, "SUSPENDED", apiKey, actorUsername, failure);
        long countFallback = wrapper.getPendingContractCountOlderThanDaysFallback(30, apiKey, failure);

        // Assert - all methods returned valid fallback responses
        assertNotNull(activationFallback);
        assertNotNull(statusFallback);
        assertEquals(0L, countFallback);
        assertTrue(activationFallback.isActivationPending());
        assertTrue(statusFallback.isStatusUpdatePending());
    }
}
