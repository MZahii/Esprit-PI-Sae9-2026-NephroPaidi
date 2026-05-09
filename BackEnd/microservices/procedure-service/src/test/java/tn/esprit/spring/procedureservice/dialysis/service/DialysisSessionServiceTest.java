package tn.esprit.spring.procedureservice.dialysis.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisSessionRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisSessionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("DialysisSessionService - Idempotency Tests")
class DialysisSessionServiceTest {

    @Mock
    private DialysisSessionRepository repository;

    @Mock
    private DialysisPlanService planService;

    @InjectMocks
    private DialysisSessionService service;

    private DialysisPlan testPlan;
    private CreateDialysisSessionRequest requestWithKey;
    private CreateDialysisSessionRequest requestWithoutKey;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test plan
        testPlan = new DialysisPlan();
        testPlan.setId(1L);
        testPlan.setPatientId("P123");
        
        // Setup requests
        requestWithKey = new CreateDialysisSessionRequest(
            1L,
            "P123",
            "C456",
            "A789",
            LocalDateTime.now(),
            "Test session",
            "idempotency-key-001"
        );
        
        requestWithoutKey = new CreateDialysisSessionRequest(
            1L,
            "P123",
            "C456",
            "A789",
            LocalDateTime.now(),
            "Test session",
            null
        );
    }

    @Test
    @DisplayName("Test 1: Create session without idempotency key (should create new)")
    void testCreateSessionWithoutIdempotencyKey() {
        // Arrange
        when(planService.getById(1L)).thenReturn(testPlan);
        when(repository.save(any(DialysisSession.class))).thenAnswer(invocation -> {
            DialysisSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // Act
        DialysisSession result = service.create(requestWithoutKey);

        // Assert
        assertNotNull(result);
        assertEquals("P123", result.getPatientId());
        verify(repository, times(1)).save(any(DialysisSession.class));
        verify(repository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("Test 2: Create session with idempotency key (should create new)")
    void testCreateSessionWithIdempotencyKey() {
        // Arrange
        when(planService.getById(1L)).thenReturn(testPlan);
        when(repository.findByIdempotencyKey("idempotency-key-001")).thenReturn(Optional.empty());
        when(repository.save(any(DialysisSession.class))).thenAnswer(invocation -> {
            DialysisSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // Act
        DialysisSession result = service.create(requestWithKey);

        // Assert
        assertNotNull(result);
        assertEquals("idempotency-key-001", result.getIdempotencyKey());
        assertEquals("P123", result.getPatientId());
        verify(repository, times(1)).findByIdempotencyKey("idempotency-key-001");
        verify(repository, times(1)).save(any(DialysisSession.class));
    }

    @Test
    @DisplayName("Test 3: Duplicate request with same idempotency key (should return existing)")
    void testDuplicateRequestReturnsExistingSession() {
        // Arrange
        DialysisSession existingSession = new DialysisSession();
        existingSession.setId(1L);
        existingSession.setIdempotencyKey("idempotency-key-001");
        existingSession.setPatientId("P123");
        existingSession.setPlan(testPlan);
        
        when(repository.findByIdempotencyKey("idempotency-key-001"))
            .thenReturn(Optional.of(existingSession));

        // Act
        DialysisSession result = service.create(requestWithKey);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("idempotency-key-001", result.getIdempotencyKey());
        // Verify existing session was returned, no save was called
        verify(repository, times(1)).findByIdempotencyKey("idempotency-key-001");
        verify(repository, never()).save(any(DialysisSession.class));
    }

    @Test
    @DisplayName("Test 4: Create with empty idempotency key (should create new each time)")
    void testCreateWithEmptyIdempotencyKeyCreatesNewEachTime() {
        // Arrange
        CreateDialysisSessionRequest emptyKeyRequest = new CreateDialysisSessionRequest(
            1L,
            "P123",
            "C456",
            "A789",
            LocalDateTime.now(),
            "Test session",
            ""  // Empty key
        );
        
        when(planService.getById(1L)).thenReturn(testPlan);
        when(repository.save(any(DialysisSession.class))).thenAnswer(invocation -> {
            DialysisSession session = invocation.getArgument(0);
            session.setId(Math.random() > 0.5 ? 1L : 2L);
            return session;
        });

        // Act - Create twice with empty key
        DialysisSession result1 = service.create(emptyKeyRequest);
        DialysisSession result2 = service.create(emptyKeyRequest);

        // Assert - Both should create new sessions (empty key is treated as no key)
        assertNotNull(result1);
        assertNotNull(result2);
        verify(repository, times(2)).save(any(DialysisSession.class));
        verify(repository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("Test 5: Session persists idempotency key when saved")
    void testIdempotencyKeyIsPersisted() {
        // Arrange
        when(planService.getById(1L)).thenReturn(testPlan);
        when(repository.findByIdempotencyKey("idempotency-key-001")).thenReturn(Optional.empty());
        when(repository.save(any(DialysisSession.class))).thenAnswer(invocation -> {
            DialysisSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // Act
        DialysisSession result = service.create(requestWithKey);

        // Assert
        assertNotNull(result);
        assertEquals("idempotency-key-001", result.getIdempotencyKey());
        
        // Verify save was called and the session had idempotency key set
        verify(repository, times(1)).save(result);
    }
}
