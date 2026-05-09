package tn.esprit.spring.clinicalservice.appointment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentCancelRequest;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentCreateRequest;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentResponse;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentUpdateRequest;
import tn.esprit.spring.clinicalservice.appointment.dto.StartConsultationResponse;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import tn.esprit.spring.clinicalservice.appointment.repository.AppointmentRepository;
import tn.esprit.spring.clinicalservice.appointment.service.impl.AppointmentServiceImpl;
import tn.esprit.spring.clinicalservice.audit.AuditService;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.security.ActorResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl - Appointment Flows")
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private PatientDirectoryClient patientDirectoryClient;

    @Mock
    private AuditService auditService;

    @Mock
    private ActorResolver actorResolver;

    @InjectMocks
    private AppointmentServiceImpl service;

    @Test
    void createRejectsMissingScheduledAt() {
        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setPatientId(1L);
        request.setDoctorId(UUID.randomUUID());

        assertThrows(ResponseStatusException.class, () -> service.create(request));
    }

    @Test
    void createStoresAppointmentWhenSlotsAreFree() {
        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setPatientId(1L);
        request.setDoctorId(UUID.randomUUID());
        request.setScheduledAt(LocalDateTime.now().plusDays(1));
        request.setDurationMinutes(30);
        request.setReason("Checkup");

        Appointment saved = Appointment.builder()
                .id(UUID.randomUUID())
                .patientId(1L)
                .doctorId(request.getDoctorId())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(30)
                .reason("Checkup")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.existsOverlapping(any(), any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentResponse response = service.create(request);

        assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void startConsultationCreatesConsultationAndConfirmsAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patientId(2L)
                .doctorId(doctorId)
                .scheduledAt(LocalDateTime.now().plusHours(2))
                .durationMinutes(30)
                .status(AppointmentStatus.SCHEDULED)
                .build();
        Consultation consultation = Consultation.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointmentId)
                .patientId(2L)
                .doctorId(doctorId)
                .dateTime(appointment.getScheduledAt())
                .status(ConsultationStatus.IN_PROGRESS)
                .build();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(consultationRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(consultationRepository.save(any(Consultation.class))).thenReturn(consultation);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        StartConsultationResponse response = service.startConsultation(appointmentId, doctorId);

        assertEquals(consultation.getId(), response.getConsultationId());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void cancelMarksAppointmentCancelled() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patientId(3L)
                .doctorId(UUID.randomUUID())
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(30)
                .status(AppointmentStatus.SCHEDULED)
                .build();
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancel(appointment.getId(), new AppointmentCancelRequest("No longer needed"));

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("No longer needed", appointment.getCancellationReason());
    }

    @Test
    void deleteArchivesAndAuditsAppointment() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patientId(4L)
                .doctorId(UUID.randomUUID())
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .durationMinutes(30)
                .status(AppointmentStatus.SCHEDULED)
                .build();
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(consultationRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());
        when(actorResolver.resolveCurrent()).thenReturn(new tn.esprit.spring.clinicalservice.security.ActorInfo("doc-1", "dr.amine", "DOCTOR"));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(appointment.getId());

        assertEquals(AppointmentStatus.ARCHIVED, appointment.getStatus());
        verify(auditService).record(anyString(), any(), anyString(), anyString());
    }
}