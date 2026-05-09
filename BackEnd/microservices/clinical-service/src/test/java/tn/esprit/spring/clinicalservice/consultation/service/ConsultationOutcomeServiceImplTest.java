package tn.esprit.spring.clinicalservice.consultation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationOutcomeResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationOutcome;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationOutcomeRepository;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.service.impl.ConsultationOutcomeServiceImpl;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultationOutcomeServiceImpl - Outcome Flows")
class ConsultationOutcomeServiceImplTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationOutcomeRepository outcomeRepository;

    @Mock
    private GuardianNotificationService guardianNotificationService;

    @InjectMocks
    private ConsultationOutcomeServiceImpl service;

    @Test
    void updateNotesCreatesOutcomeWhenMissing() {
        UUID consultationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
                .id(consultationId)
                .doctorId(doctorId)
                .patientId(7L)
                .status(ConsultationStatus.COMPLETED)
                .build();
        ConsultationOutcome saved = ConsultationOutcome.builder()
                .consultationId(consultationId)
                .notes("New note")
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(outcomeRepository.findByConsultationId(consultationId)).thenReturn(Optional.empty());
        when(outcomeRepository.save(any(ConsultationOutcome.class))).thenReturn(saved);

        ConsultationOutcomeResponse response = service.updateNotes(consultationId, doctorId, "New note");

        assertEquals("New note", response.getNotes());
    }

    @Test
    void updatePrescriptionsNotifiesGuardiansWhenChanged() {
        UUID consultationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
                .id(consultationId)
                .doctorId(doctorId)
                .patientId(7L)
                .status(ConsultationStatus.COMPLETED)
                .build();
        ConsultationOutcome outcome = ConsultationOutcome.builder()
                .consultationId(consultationId)
                .prescriptions("Old")
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(outcomeRepository.findByConsultationId(consultationId)).thenReturn(Optional.of(outcome));
        when(outcomeRepository.save(any(ConsultationOutcome.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationOutcomeResponse response = service.updatePrescriptions(consultationId, doctorId, "New");

        assertEquals("New", response.getPrescriptions());
        verify(guardianNotificationService).notifyGuardians(any(), any(), anyString());
    }

    @Test
    void getOutcomeReturnsEmptyResponseWhenMissing() {
        UUID consultationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
                .id(consultationId)
                .doctorId(doctorId)
                .patientId(7L)
                .status(ConsultationStatus.COMPLETED)
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(outcomeRepository.findByConsultationId(consultationId)).thenReturn(Optional.empty());

        ConsultationOutcomeResponse response = service.getOutcome(consultationId, doctorId);

        assertEquals(consultationId, response.getConsultationId());
        assertEquals(null, response.getNotes());
    }

    @Test
    void getOutcomeRejectsArchivedConsultation() {
        UUID consultationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Consultation consultation = Consultation.builder()
                .id(consultationId)
                .doctorId(doctorId)
                .patientId(7L)
                .status(ConsultationStatus.ARCHIVED)
                .build();

        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));

        assertThrows(ResponseStatusException.class, () -> service.getOutcome(consultationId, doctorId));
        verify(outcomeRepository, never()).findByConsultationId(any());
    }
}