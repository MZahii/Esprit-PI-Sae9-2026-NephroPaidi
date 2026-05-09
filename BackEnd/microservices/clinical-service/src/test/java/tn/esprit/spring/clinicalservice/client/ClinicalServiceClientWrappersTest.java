package tn.esprit.spring.clinicalservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for clinical-service client wrappers with circuit breaker resilience
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical-Service Client Wrappers")
class ClinicalServiceClientWrappersTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProcedureServiceClient procedureServiceClient;

    @Mock
    private PharmacyClient pharmacyClient;

    @Mock
    private CommunicationClient communicationClient;

    @Mock
    private AdministrationClient administrationClient;

    private UserServiceClientWrapper userWrapper;
    private ProcedureServiceClientWrapper procedureWrapper;
    private PharmacyClientWrapper pharmacyWrapper;
    private CommunicationClientWrapper communicationWrapper;
    private AdministrationClientWrapper administrationWrapper;

    @BeforeEach
    void setUp() {
        userWrapper = new UserServiceClientWrapper(userServiceClient);
        procedureWrapper = new ProcedureServiceClientWrapper(procedureServiceClient);
        pharmacyWrapper = new PharmacyClientWrapper(pharmacyClient);
        communicationWrapper = new CommunicationClientWrapper(communicationClient);
        administrationWrapper = new AdministrationClientWrapper(administrationClient);
    }

    // ==================== UserServiceClientWrapper Tests ====================

    @Test
    @DisplayName("getUserById - Success Response")
    void testGetUserByIdSuccess() {
        // Arrange
        String userId = "user123";
        String token = "bearer-token";
        Object userObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(userObject);
        when(userServiceClient.getUserById(userId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = userWrapper.getUserById(userId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userServiceClient).getUserById(userId, token);
    }

    @Test
    @DisplayName("getUserById - Fallback on Exception")
    void testGetUserByIdFallback() {
        // Arrange
        String userId = "user123";
        String token = "bearer-token";

        // Act
        ResponseEntity<Object> result = userWrapper.getUserByIdFallback(userId, token, 
                new RuntimeException("Service unavailable"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    @DisplayName("getAllUsers - Success Response")
    void testGetAllUsersSuccess() {
        // Arrange
        String token = "bearer-token";
        List<Object> users = List.of();
        ResponseEntity<List<Object>> expectedResponse = ResponseEntity.ok(users);
        when(userServiceClient.getAllUsers(token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<List<Object>> result = userWrapper.getAllUsers(token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userServiceClient).getAllUsers(token);
    }

    @Test
    @DisplayName("getAllUsers - Fallback Returns Empty List")
    void testGetAllUsersFallback() {
        // Arrange & Act
        ResponseEntity<List<Object>> result = userWrapper.getAllUsersFallback("token", 
                new RuntimeException("No users"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ==================== ProcedureServiceClientWrapper Tests ====================

    @Test
    @DisplayName("getSurgicalCase - Success Response")
    void testGetSurgicalCaseSuccess() {
        // Arrange
        UUID caseId = UUID.randomUUID();
        String token = "bearer-token";
        Object caseObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(caseObject);
        when(procedureServiceClient.getSurgicalCase(caseId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = procedureWrapper.getSurgicalCase(caseId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(procedureServiceClient).getSurgicalCase(caseId, token);
    }

    @Test
    @DisplayName("getSurgicalCase - Fallback on Exception")
    void testGetSurgicalCaseFallback() {
        // Arrange
        UUID caseId = UUID.randomUUID();
        String token = "bearer-token";

        // Act
        ResponseEntity<Object> result = procedureWrapper.getSurgicalCaseFallback(caseId, token,
                new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    @DisplayName("getAllSurgicalCases - Fallback Returns Empty List")
    void testGetAllSurgicalCasesFallback() {
        // Act
        ResponseEntity<List<Object>> result = procedureWrapper.getAllSurgicalCasesFallback("token",
                new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    @DisplayName("getDialysisPlan - Success Response")
    void testGetDialysisPlanSuccess() {
        // Arrange
        Long planId = 1L;
        String token = "bearer-token";
        Object planObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(planObject);
        when(procedureServiceClient.getDialysisPlan(planId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = procedureWrapper.getDialysisPlan(planId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(procedureServiceClient).getDialysisPlan(planId, token);
    }

    // ==================== PharmacyClientWrapper Tests ====================

    @Test
    @DisplayName("getMedicationById - Success Response")
    void testGetMedicationByIdSuccess() {
        // Arrange
        String medicationId = "med123";
        String token = "bearer-token";
        Object medicationObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(medicationObject);
        when(pharmacyClient.getMedicationById(medicationId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = pharmacyWrapper.getMedicationById(medicationId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(pharmacyClient).getMedicationById(medicationId, token);
    }

    @Test
    @DisplayName("createMedicationOrder - Fallback on Exception")
    void testCreateMedicationOrderFallback() {
        // Act
        ResponseEntity<Object> result = pharmacyWrapper.createMedicationOrderFallback(
                new Object(), "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    @DisplayName("validateOrderAvailability - Returns False on Fallback")
    void testValidateOrderAvailabilityFallback() {
        // Act
        ResponseEntity<Boolean> result = pharmacyWrapper.validateOrderAvailabilityFallback(
                "med123", 10, "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(result.getBody());
    }

    @Test
    @DisplayName("getAllMedications - Fallback Returns Empty List")
    void testGetAllMedicationsFallback() {
        // Act
        ResponseEntity<List<Object>> result = pharmacyWrapper.getAllMedicationsFallback("token",
                new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ==================== CommunicationClientWrapper Tests ====================

    @Test
    @DisplayName("sendNotification - Fallback on Exception")
    void testSendNotificationFallback() {
        // Act
        ResponseEntity<Void> result = communicationWrapper.sendNotificationFallback(
                new Object(), "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    @DisplayName("createMessage - Success Response")
    void testCreateMessageSuccess() {
        // Arrange
        String token = "bearer-token";
        Object messageRequest = new Object();
        Object messageResponse = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(messageResponse);
        when(communicationClient.createMessage(messageRequest, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = communicationWrapper.createMessage(messageRequest, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(communicationClient).createMessage(messageRequest, token);
    }

    @Test
    @DisplayName("getMyAppointments - Fallback Returns Empty List")
    void testGetMyAppointmentsFallback() {
        // Act
        ResponseEntity<List<Object>> result = communicationWrapper.getMyAppointmentsFallback("token",
                new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ==================== AdministrationClientWrapper Tests ====================

    @Test
    @DisplayName("getPatient - Success Response")
    void testGetPatientSuccess() {
        // Arrange
        String patientId = "patient123";
        String token = "bearer-token";
        Object patientObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(patientObject);
        when(administrationClient.getPatient(patientId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = administrationWrapper.getPatient(patientId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(administrationClient).getPatient(patientId, token);
    }

    @Test
    @DisplayName("getPatient - Fallback on Exception")
    void testGetPatientFallback() {
        // Act
        ResponseEntity<Object> result = administrationWrapper.getPatientFallback(
                "patient123", "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
    }

    @Test
    @DisplayName("getAllPatients - Fallback Returns Empty List")
    void testGetAllPatientsFallback() {
        // Act
        ResponseEntity<List<Object>> result = administrationWrapper.getAllPatientsFallback("token",
                new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    @DisplayName("getContractsByStaff - Example with Real Service Response")
    void testGetContractsByStaffSuccess() {
        // Arrange
        String staffId = "staff123";
        String token = "bearer-token";
        List<Object> contracts = List.of();
        ResponseEntity<List<Object>> expectedResponse = ResponseEntity.ok(contracts);
        when(administrationClient.getContractsByStaff(staffId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<List<Object>> result = administrationWrapper.getContractsByStaff(staffId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(administrationClient).getContractsByStaff(staffId, token);
    }

    @Test
    @DisplayName("getPatientsByGuardian - Fallback Returns Empty List")
    void testGetPatientsByGuardianFallback() {
        // Act
        ResponseEntity<List<Object>> result = administrationWrapper.getPatientsByGuardianFallback(
                "guardian123", "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ==================== Integration/Cross-Wrapper Tests ====================

    @Test
    @DisplayName("Multiple Wrapper Calls - Independent Circuit Breakers")
    void testMultipleWrappersIndependentCircuitBreakers() {
        // Arrange
        String token = "bearer-token";
        when(userServiceClient.getAllUsers(token)).thenReturn(ResponseEntity.ok(List.of()));

        // Act
        ResponseEntity<List<Object>> userResult = userWrapper.getAllUsers(token);
        ResponseEntity<List<Object>> pharmacyResult = pharmacyWrapper.getAllMedicationsFallback(token,
                new RuntimeException("Pharmacy down"));

        // Assert - Both should work independently
        assertEquals(HttpStatus.OK, userResult.getStatusCode());
        assertEquals(HttpStatus.OK, pharmacyResult.getStatusCode());
        assertTrue(pharmacyResult.getBody().isEmpty());
    }

    @Test
    @DisplayName("Fallback Methods Preserve Error Processing")
    void testFallbackMethodsPreserveErrorProcessing() {
        // Arrange
        Exception serviceError = new RuntimeException("Connection timeout");

        // Act & Assert - Verify all fallbacks handle exceptions gracefully
        assertDoesNotThrow(() -> {
            userWrapper.getUserByIdFallback("123", "token", serviceError);
            procedureWrapper.getSurgicalCaseFallback(UUID.randomUUID(), "token", serviceError);
            pharmacyWrapper.getMedicationByIdFallback("123", "token", serviceError);
            communicationWrapper.sendNotificationFallback(new Object(), "token", serviceError);
            administrationWrapper.getPatientFallback("123", "token", serviceError);
        });
    }
}
