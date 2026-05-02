package tn.esprit.spring.clinicalservice.consultation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.clinicalservice.audit.AuditService;
import tn.esprit.spring.clinicalservice.client.AdministrationClient;
import tn.esprit.spring.clinicalservice.client.CommunicationClient;
import tn.esprit.spring.clinicalservice.client.PharmacyClient;
import tn.esprit.spring.clinicalservice.client.UserServiceClient;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationCreateRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationUpdateRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.exception.ConsultationValidationException;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.service.impl.ConsultationServiceImpl;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.security.ActorInfo;
import tn.esprit.spring.clinicalservice.security.ActorResolver;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ConsultationService - Doctor Validation Tests")
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private PatientDirectoryClient patientDirectoryClient;

    @Mock
    private AuditService auditService;

    @Mock
    private ActorResolver actorResolver;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AdministrationClient administrationClient;

    @Mock
    private PharmacyClient pharmacyClient;

    @Mock
    private CommunicationClient communicationClient;

    @InjectMocks
    private ConsultationServiceImpl consultationService;

    private UUID doctorId;
    private UUID appointmentId;
    private ConsultationCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        doctorId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        
        validRequest = new ConsultationCreateRequest();
        validRequest.setPatientId(1L);
        validRequest.setDateTime(LocalDateTime.now().plusHours(1));
        validRequest.setAppointmentId(appointmentId);
    }

    @Test
    @DisplayName("Test 1: Create consultation - valid doctor ID succeeds")
    void testCreateConsultationWithValidDoctorId() {
        // Arrange
        Consultation savedConsultation = Consultation.builder()
            .id(UUID.randomUUID())
            .patientId(1L)
            .doctorId(doctorId)
            .dateTime(validRequest.getDateTime())
            .appointmentId(appointmentId)
            .status(ConsultationStatus.OPEN)
            .build();
        
        when(consultationRepository.save(any(Consultation.class))).thenReturn(savedConsultation);

        // Act
        ConsultationResponse result = consultationService.create(validRequest, doctorId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getPatientId());
        assertEquals(doctorId, result.getDoctorId());
        assertEquals(ConsultationStatus.OPEN, result.getStatus());
        
        verify(consultationRepository, times(1)).save(any(Consultation.class));
    }

    @Test
    @DisplayName("Test 2: Create consultation - null doctor ID throws ConsultationValidationException")
    void testCreateConsultationWithNullDoctorId() {
        // Act & Assert
        assertThrows(ConsultationValidationException.class, () -> {
            consultationService.create(validRequest, null);
        });
        
        verify(consultationRepository, never()).save(any(Consultation.class));
    }

    @Test
    @DisplayName("Test 3: Create consultation - appointment_id stored for referential tracking")
    void testCreateConsultationStoresAppointmentReference() {
        // Arrange
        Consultation savedConsultation = Consultation.builder()
            .id(UUID.randomUUID())
            .patientId(1L)
            .doctorId(doctorId)
            .dateTime(validRequest.getDateTime())
            .appointmentId(appointmentId)
            .status(ConsultationStatus.OPEN)
            .build();
        
        when(consultationRepository.save(any(Consultation.class))).thenReturn(savedConsultation);

        // Act
        ConsultationResponse result = consultationService.create(validRequest, doctorId);

        // Assert
        assertNotNull(result);
        assertEquals(appointmentId, result.getAppointmentId());
        
        verify(consultationRepository, times(1)).save(argThat(consultation -> 
            consultation.getAppointmentId().equals(appointmentId)
        ));
    }

    @Test
    @DisplayName("Test 4: Create consultation - appointment_id can be null")
    void testCreateConsultationWithoutAppointment() {
        // Arrange
        ConsultationCreateRequest noAppointmentRequest = new ConsultationCreateRequest();
        noAppointmentRequest.setPatientId(1L);
        noAppointmentRequest.setDateTime(LocalDateTime.now().plusHours(1));
        noAppointmentRequest.setAppointmentId(null);
        
        Consultation savedConsultation = Consultation.builder()
            .id(UUID.randomUUID())
            .patientId(1L)
            .doctorId(doctorId)
            .dateTime(noAppointmentRequest.getDateTime())
            .appointmentId(null)
            .status(ConsultationStatus.OPEN)
            .build();
        
        when(consultationRepository.save(any(Consultation.class))).thenReturn(savedConsultation);

        // Act
        ConsultationResponse result = consultationService.create(noAppointmentRequest, doctorId);

        // Assert
        assertNotNull(result);
        assertNull(result.getAppointmentId());
        verify(consultationRepository, times(1)).save(any(Consultation.class));
    }

    @Test
    @DisplayName("Test 5: Create consultation - initial status is OPEN")
    void testConsultationInitialStatusIsOpen() {
        // Arrange
        Consultation savedConsultation = Consultation.builder()
            .id(UUID.randomUUID())
            .patientId(1L)
            .doctorId(doctorId)
            .dateTime(validRequest.getDateTime())
            .status(ConsultationStatus.OPEN)
            .build();
        
        when(consultationRepository.save(any(Consultation.class))).thenReturn(savedConsultation);

        // Act
        ConsultationResponse result = consultationService.create(validRequest, doctorId);

        // Assert
        assertEquals(ConsultationStatus.OPEN, result.getStatus());
    }

    @Test
    @DisplayName("Test 6: Create consultation - FK constraint enforces appointment referential integrity")
    void testConsultationAppointmentReferentialIntegrity() {
        // Arrange - consultation with appointment reference
        Consultation savedConsultation = Consultation.builder()
            .id(UUID.randomUUID())
            .patientId(1L)
            .doctorId(doctorId)
            .dateTime(validRequest.getDateTime())
            .appointmentId(appointmentId)
            .status(ConsultationStatus.OPEN)
            .build();
        
        when(consultationRepository.save(any(Consultation.class))).thenReturn(savedConsultation);

        // Act
        ConsultationResponse result = consultationService.create(validRequest, doctorId);

        // Assert - appointment reference is preserved
        assertNotNull(result);
        assertEquals(appointmentId, result.getAppointmentId());
    }

        @Test
        @DisplayName("Test 7: Update consultation - owner can change status and date")
        void testUpdateConsultationByOwner() {
        UUID consultationId = UUID.randomUUID();
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2);
        Consultation consultation = Consultation.builder()
            .id(consultationId)
            .patientId(3L)
            .doctorId(doctorId)
            .dateTime(LocalDateTime.now())
            .status(ConsultationStatus.OPEN)
            .build();
        ConsultationUpdateRequest request = ConsultationUpdateRequest.builder()
            .dateTime(newDateTime)
            .status(ConsultationStatus.COMPLETED)
            .build();
        when(consultationRepository.findById(consultationId)).thenReturn(java.util.Optional.of(consultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.update(consultationId, doctorId, request);

        assertEquals(newDateTime, response.getDateTime());
        assertEquals(ConsultationStatus.COMPLETED, response.getStatus());
        verify(consultationRepository).save(any(Consultation.class));
        }

        @Test
        @DisplayName("Test 8: Update consultation - other doctor is forbidden")
        void testUpdateConsultationByOtherDoctorIsForbidden() {
        UUID consultationId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
            .id(consultationId)
            .patientId(3L)
            .doctorId(UUID.randomUUID())
            .dateTime(LocalDateTime.now())
            .status(ConsultationStatus.OPEN)
            .build();
        ConsultationUpdateRequest request = ConsultationUpdateRequest.builder()
            .status(ConsultationStatus.COMPLETED)
            .build();
        when(consultationRepository.findById(consultationId)).thenReturn(java.util.Optional.of(consultation));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> consultationService.update(consultationId, doctorId, request));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(consultationRepository, never()).save(any(Consultation.class));
        }

        @Test
        @DisplayName("Test 9: listAll excludes archived consultations by default")
        void testListAllExcludesArchivedByDefault() {
        UUID openConsultationId = UUID.randomUUID();
        UUID archivedConsultationId = UUID.randomUUID();
        Consultation openConsultation = Consultation.builder()
            .id(openConsultationId)
            .patientId(11L)
            .doctorId(doctorId)
            .dateTime(LocalDateTime.now().plusDays(1))
            .status(ConsultationStatus.OPEN)
            .build();
        Consultation archivedConsultation = Consultation.builder()
            .id(archivedConsultationId)
            .patientId(12L)
            .doctorId(doctorId)
            .dateTime(LocalDateTime.now().plusDays(2))
            .status(ConsultationStatus.ARCHIVED)
            .build();
        when(consultationRepository.findAll()).thenReturn(List.of(openConsultation, archivedConsultation));
        when(patientDirectoryClient.getPatientsByIds(any())).thenReturn(Map.of(
            11L, new PatientSummary(11L, "Youssef", "Ben Salah"),
            12L, new PatientSummary(12L, "Sara", "Trabelsi")
        ));

        List<ConsultationResponse> responses = consultationService.listAll(null, null, null, null);

        assertEquals(1, responses.size());
        assertEquals(openConsultationId, responses.get(0).getId());
        assertEquals("Youssef Ben Salah", responses.get(0).getPatientName());
        }

        @Test
        @DisplayName("Test 10: cancel archives the consultation and records the actor")
        void testCancelArchivesConsultation() {
        UUID consultationId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
            .id(consultationId)
            .patientId(15L)
            .doctorId(doctorId)
            .dateTime(LocalDateTime.now().plusDays(1))
            .status(ConsultationStatus.OPEN)
            .build();
        when(consultationRepository.findById(consultationId)).thenReturn(java.util.Optional.of(consultation));
        when(actorResolver.resolveCurrent()).thenReturn(new ActorInfo("doctor-1", "alice", "DOCTOR"));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consultationService.cancel(consultationId, doctorId);

        verify(consultationRepository).save(any(Consultation.class));
        verify(auditService).record("CONSULTATION", consultationId, "ARCHIVE", "Archived consultation");
        assertEquals(ConsultationStatus.ARCHIVED, consultation.getStatus());
        assertEquals("alice (doctor-1)", consultation.getArchivedBy());
        assertNotNull(consultation.getArchivedAt());
        }
}
