package tn.esprit.spring.clinicalservice.consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationResponse;
import tn.esprit.spring.clinicalservice.consultation.dto.GuardianOutcomeResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationOutcome;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationOutcomeRepository;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationOutcomeRepository outcomeRepository;
    private final PatientDirectoryClient patientDirectoryClient;

    public List<ConsultationResponse> listConsultations(Long guardianUserId, Long patientId, ConsultationStatus status) {
        if (guardianUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guardianUserId is required");
        }

        List<Long> guardianPatientIds = patientDirectoryClient.getPatientIdsByGuardianUserId(guardianUserId);
        if (guardianPatientIds == null || guardianPatientIds.isEmpty()) {
            return List.of();
        }

        List<Long> scopedPatientIds = guardianPatientIds;
        if (patientId != null) {
            if (!guardianPatientIds.contains(patientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your patient");
            }
            scopedPatientIds = List.of(patientId);
        }

        List<Consultation> consultations = status == null
                ? consultationRepository.findByPatientIdIn(scopedPatientIds)
                : consultationRepository.findByPatientIdInAndStatus(scopedPatientIds, status);

        Map<Long, PatientSummary> patientMap = patientDirectoryClient.getPatientsByIds(extractPatientIds(consultations));

        return consultations.stream()
                .filter(consultation -> consultation.getStatus() != ConsultationStatus.ARCHIVED)
                .map(consultation -> toResponse(consultation, patientMap.get(consultation.getPatientId())))
                .toList();
    }

    public GuardianOutcomeResponse getOutcome(UUID consultationId, Long guardianUserId) {
        if (guardianUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guardianUserId is required");
        }

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }

        List<Long> patientIds = patientDirectoryClient.getPatientIdsByGuardianUserId(guardianUserId);
        if (patientIds == null || !patientIds.contains(consultation.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your patient");
        }

        ConsultationOutcome outcome = outcomeRepository.findByConsultationId(consultationId).orElse(null);
        if (outcome == null) {
            return GuardianOutcomeResponse.builder()
                    .consultationId(consultationId)
                    .build();
        }

        return GuardianOutcomeResponse.builder()
                .consultationId(consultationId)
                .prescriptions(outcome.getPrescriptions())
                .treatmentPlan(outcome.getTreatmentPlan())
                .updatedAt(outcome.getUpdatedAt())
                .build();
    }

    private ConsultationResponse toResponse(Consultation consultation, PatientSummary patientSummary) {
        return ConsultationResponse.builder()
                .id(consultation.getId())
                .patientId(consultation.getPatientId())
                .patientName(formatPatientName(patientSummary))
                .doctorId(consultation.getDoctorId())
                .dateTime(consultation.getDateTime())
                .appointmentId(consultation.getAppointmentId())
                .startedAt(consultation.getStartedAt())
                .status(consultation.getStatus())
                .archivedAt(consultation.getArchivedAt())
                .archivedBy(consultation.getArchivedBy())
                .build();
    }

    private Collection<Long> extractPatientIds(List<Consultation> consultations) {
        return consultations.stream()
                .map(Consultation::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String formatPatientName(PatientSummary summary) {
        if (summary == null) {
            return null;
        }
        String first = summary.getFirstName() == null ? "" : summary.getFirstName().trim();
        String last = summary.getLastName() == null ? "" : summary.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
