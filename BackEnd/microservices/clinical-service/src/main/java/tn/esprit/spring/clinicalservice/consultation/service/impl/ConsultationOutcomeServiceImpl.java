package tn.esprit.spring.clinicalservice.consultation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationOutcomeResponse;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationLabRequestsResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationOutcome;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationOutcomeRepository;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.service.ConsultationOutcomeService;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationService;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationType;

import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationOutcomeServiceImpl implements ConsultationOutcomeService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationOutcomeRepository outcomeRepository;
    private final GuardianNotificationService guardianNotificationService;

    @Override
    public ConsultationOutcomeResponse updateNotes(UUID consultationId, UUID doctorId, String content) {
        ConsultationOutcome outcome = getOrCreateOutcome(consultationId, doctorId);
        outcome.setNotes(content);
        return mapToResponse(outcomeRepository.save(outcome));
    }

    @Override
    public ConsultationOutcomeResponse updateDiagnosis(UUID consultationId, UUID doctorId, String content) {
        ConsultationOutcome outcome = getOrCreateOutcome(consultationId, doctorId);
        outcome.setDiagnosis(content);
        return mapToResponse(outcomeRepository.save(outcome));
    }

    @Override
    public ConsultationOutcomeResponse updatePrescriptions(UUID consultationId, UUID doctorId, String content) {
        ConsultationOutcome outcome = getOrCreateOutcome(consultationId, doctorId);
        String before = outcome.getPrescriptions();
        outcome.setPrescriptions(content);
        ConsultationOutcome saved = outcomeRepository.save(outcome);
        notifyIfChanged(consultationId, before, content, GuardianNotificationType.PRESCRIPTIONS_UPDATED,
                "Doctor updated prescriptions.");
        return mapToResponse(saved);
    }

    @Override
    public ConsultationOutcomeResponse updateLabRequests(UUID consultationId, UUID doctorId, String content) {
        ConsultationOutcome outcome = getOrCreateOutcome(consultationId, doctorId);
        outcome.setLabRequests(content);
        return mapToResponse(outcomeRepository.save(outcome));
    }

    @Override
    public ConsultationLabRequestsResponse updateSharedLabRequests(UUID consultationId, String content) {
        ConsultationOutcome outcome = getOrCreateSharedOutcome(consultationId);
        outcome.setLabRequests(content);
        return mapLabRequestsResponse(outcomeRepository.save(outcome));
    }

    @Override
    public ConsultationOutcomeResponse updateTreatmentPlan(UUID consultationId, UUID doctorId, String content) {
        ConsultationOutcome outcome = getOrCreateOutcome(consultationId, doctorId);
        String before = outcome.getTreatmentPlan();
        outcome.setTreatmentPlan(content);
        ConsultationOutcome saved = outcomeRepository.save(outcome);
        notifyIfChanged(consultationId, before, content, GuardianNotificationType.TREATMENT_PLAN_UPDATED,
                "Doctor updated treatment plan.");
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationOutcomeResponse getOutcome(UUID consultationId, UUID doctorId) {
        ensureOwnership(consultationId, doctorId);
        ConsultationOutcome outcome = outcomeRepository.findByConsultationId(consultationId)
                .orElse(null);
        if (outcome == null) {
            return ConsultationOutcomeResponse.builder()
                    .consultationId(consultationId)
                    .build();
        }
        return mapToResponse(outcome);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationLabRequestsResponse getSharedLabRequests(UUID consultationId) {
        ensureConsultationExistsAndActive(consultationId);
        ConsultationOutcome outcome = outcomeRepository.findByConsultationId(consultationId)
                .orElse(null);
        if (outcome == null) {
            return ConsultationLabRequestsResponse.builder()
                    .consultationId(consultationId)
                    .labRequests(null)
                    .updatedAt(null)
                    .build();
        }
        return mapLabRequestsResponse(outcome);
    }

    private ConsultationOutcome getOrCreateOutcome(UUID consultationId, UUID doctorId) {
        ensureOwnership(consultationId, doctorId);
        return outcomeRepository.findByConsultationId(consultationId)
                .orElseGet(() -> ConsultationOutcome.builder()
                        .consultationId(consultationId)
                        .build());
    }

    private ConsultationOutcome getOrCreateSharedOutcome(UUID consultationId) {
        ensureConsultationExistsAndActive(consultationId);
        return outcomeRepository.findByConsultationId(consultationId)
                .orElseGet(() -> ConsultationOutcome.builder()
                        .consultationId(consultationId)
                        .build());
    }

    private void ensureOwnership(UUID consultationId, UUID doctorId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
        if (doctorId == null || !consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }
        if (consultation.getStatus() == tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }
    }

    private void ensureConsultationExistsAndActive(UUID consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
        if (consultation.getStatus() == tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }
    }

    private ConsultationOutcomeResponse mapToResponse(ConsultationOutcome outcome) {
        return ConsultationOutcomeResponse.builder()
                .consultationId(outcome.getConsultationId())
                .notes(outcome.getNotes())
                .diagnosis(outcome.getDiagnosis())
                .prescriptions(outcome.getPrescriptions())
                .labRequests(outcome.getLabRequests())
                .treatmentPlan(outcome.getTreatmentPlan())
                .updatedAt(outcome.getUpdatedAt())
                .build();
    }

    private ConsultationLabRequestsResponse mapLabRequestsResponse(ConsultationOutcome outcome) {
        return ConsultationLabRequestsResponse.builder()
                .consultationId(outcome.getConsultationId())
                .labRequests(outcome.getLabRequests())
                .updatedAt(outcome.getUpdatedAt())
                .build();
    }

    private void notifyIfChanged(UUID consultationId, String before, String after, GuardianNotificationType type, String message) {
        if (after == null || after.isBlank()) {
            return;
        }
        if (Objects.equals(before, after)) {
            return;
        }
        guardianNotificationService.notifyGuardians(consultationId, type, message);
    }
}
