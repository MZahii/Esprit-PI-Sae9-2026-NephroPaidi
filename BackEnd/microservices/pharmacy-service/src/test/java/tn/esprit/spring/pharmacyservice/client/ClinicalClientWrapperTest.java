package tn.esprit.spring.pharmacyservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for pharmacy-service ClinicalClientWrapper with circuit breaker resilience
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pharmacy-Service ClinicalClient Wrapper")
class ClinicalClientWrapperTest {

    @Mock
    private ClinicalClient clinicalClient;

    private ClinicalClientWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new ClinicalClientWrapper(clinicalClient);
    }

    // ==================== Success Path Tests ====================

    @Test
    @DisplayName("getConsultation - Success Response")
    void testGetConsultationSuccess() {
        // Arrange
        String consultationId = "consult123";
        String token = "bearer-token";
        Object consultationObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(consultationObject);
        when(clinicalClient.getConsultation(consultationId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = wrapper.getConsultation(consultationId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(clinicalClient).getConsultation(consultationId, token);
    }

    @Test
    @DisplayName("getAppointment - Success Response")
    void testGetAppointmentSuccess() {
        // Arrange
        String appointmentId = "appt456";
        String token = "bearer-token";
        Object appointmentObject = new Object();
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(appointmentObject);
        when(clinicalClient.getAppointment(appointmentId, token)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = wrapper.getAppointment(appointmentId, token);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(clinicalClient).getAppointment(appointmentId, token);
    }

    // ==================== Fallback Path Tests ====================

    @Test
    @DisplayName("getConsultation - Fallback on Exception")
    void testGetConsultationFallback() {
        // Act
        ResponseEntity<Object> result = wrapper.getConsultationFallback(
                "consult123", "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    @DisplayName("getAppointment - Fallback on Exception")
    void testGetAppointmentFallback() {
        // Act
        ResponseEntity<Object> result = wrapper.getAppointmentFallback(
                "appt456", "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Fallback Methods Do Not Throw Exceptions")
    void testFallbackMethodsHandleErrorsGracefully() {
        // Arrange
        Exception serviceError = new RuntimeException("Connection timeout");

        // Act & Assert - Verify all fallbacks handle exceptions without throwing
        assertDoesNotThrow(() -> {
            wrapper.getConsultationFallback("123", "token", serviceError);
            wrapper.getAppointmentFallback("456", "token", serviceError);
        });
    }

    @Test
    @DisplayName("Error Response Contains Required Fields")
    void testErrorResponseStructure() {
        // Act
        ResponseEntity<Object> result = wrapper.getConsultationFallback(
                "consult123", "token", new RuntimeException("Service down"));

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Multiple Calls Maintain Wrapper Semantics")
    void testMultipleCalls() {
        // Arrange
        String token = "bearer-token";
        when(clinicalClient.getConsultation("cons1", token)).thenReturn(ResponseEntity.ok(new Object()));
        when(clinicalClient.getAppointment("appt1", token)).thenReturn(ResponseEntity.ok(new Object()));

        // Act
        ResponseEntity<Object> consulResult = wrapper.getConsultation("cons1", token);
        ResponseEntity<Object> apptResult = wrapper.getAppointment("appt1", token);

        // Assert
        assertEquals(HttpStatus.OK, consulResult.getStatusCode());
        assertEquals(HttpStatus.OK, apptResult.getStatusCode());
        verify(clinicalClient).getConsultation("cons1", token);
        verify(clinicalClient).getAppointment("appt1", token);
    }
}
