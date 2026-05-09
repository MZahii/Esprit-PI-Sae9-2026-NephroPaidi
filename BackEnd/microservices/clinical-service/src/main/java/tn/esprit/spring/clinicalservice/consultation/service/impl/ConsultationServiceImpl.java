package tn.esprit.spring.clinicalservice.consultation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.audit.AuditService;
import tn.esprit.spring.clinicalservice.client.UserServiceClient;
import tn.esprit.spring.clinicalservice.client.AdministrationClient;
import tn.esprit.spring.clinicalservice.client.PharmacyClient;
import tn.esprit.spring.clinicalservice.client.CommunicationClient;
import tn.esprit.spring.clinicalservice.consultation.dto.*;
import tn.esprit.spring.clinicalservice.consultation.entity.*;
import tn.esprit.spring.clinicalservice.consultation.exception.ConsultationValidationException;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.service.ConsultationService;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.security.ActorResolver;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientDirectoryClient patientDirectoryClient;
    private final AuditService auditService;
    private final ActorResolver actorResolver;
    
    // Feign clients for inter-service communication
    private final UserServiceClient userServiceClient;
    private final AdministrationClient administrationClient;
    private final PharmacyClient pharmacyClient;
    private final CommunicationClient communicationClient;

    @Override
    public ConsultationResponse create(ConsultationCreateRequest request, UUID doctorId) {
        // Validate doctor exists (doctorId must be provided)
        if (doctorId == null) {
            throw new ConsultationValidationException("Doctor ID is required for consultation creation");
        }

        // Validate appointment (if provided) is provided with proper referential integrity
        if (request.getAppointmentId() != null) {
            // Appointment validation would happen here if appointment service is available
            // For now, we store the appointment_id for referential tracking
            log.debug("Consultation linked to appointment: {}", request.getAppointmentId());
        }

        Consultation consultation = Consultation.builder()
                .patientId(request.getPatientId())
                .doctorId(doctorId)
                .dateTime(request.getDateTime())
                .appointmentId(request.getAppointmentId())
                .status(ConsultationStatus.OPEN)
                .build();

        consultation = consultationRepository.save(consultation);

        return mapToResponse(consultation);
    }

    @Override
    public ConsultationResponse update(UUID id, UUID doctorId, ConsultationUpdateRequest request) {

        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found: " + id));

        if (!consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }

        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }

        if (request.getDateTime() != null) {
            consultation.setDateTime(request.getDateTime());
        }

        consultation.setStatus(request.getStatus());

        consultation = consultationRepository.save(consultation);

        return mapToResponse(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponse getById(UUID id, UUID doctorId) {

        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found: " + id));

        if (!consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }

        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }

        return mapToResponse(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> listMine(UUID doctorId, String patientQuery, ConsultationStatus status) {
        List<Consultation> items;

        String trimmed = patientQuery == null ? "" : patientQuery.trim();
        if (!trimmed.isEmpty()) {
            List<Long> patientIds = patientDirectoryClient.searchPatientIds(trimmed, 20);
            if (patientIds.isEmpty()) {
                return List.of();
            }
            if (status != null) {
                items = consultationRepository.findByDoctorIdAndPatientIdInAndStatus(doctorId, patientIds, status);
            } else {
                items = consultationRepository.findByDoctorIdAndPatientIdIn(doctorId, patientIds);
            }
        } else {
            if (status != null) {
                items = consultationRepository.findByDoctorIdAndStatus(doctorId, status);
            } else {
                items = consultationRepository.findByDoctorId(doctorId);
            }
        }

        if (status == null || status != ConsultationStatus.ARCHIVED) {
            items = items.stream()
                    .filter(item -> item.getStatus() != ConsultationStatus.ARCHIVED)
                    .toList();
        }

        Map<Long, PatientSummary> patientMap = patientDirectoryClient.getPatientsByIds(
                items.stream().map(Consultation::getPatientId).collect(java.util.stream.Collectors.toSet())
        );
        return items.stream()
                .map(item -> mapToResponse(item, patientMap.get(item.getPatientId())))
                .toList();
    }

    @Override
    public void cancel(UUID id, UUID doctorId) {

        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found: " + id));

        if (!consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }

        consultation.setStatus(ConsultationStatus.ARCHIVED);
        consultation.setArchivedAt(LocalDateTime.now());
        consultation.setArchivedBy(actorResolver.resolveCurrent().displayName());

        consultationRepository.save(consultation);
        auditService.record("CONSULTATION", consultation.getId(), "ARCHIVE", "Archived consultation");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> listAll(Long patientId, ConsultationStatus status, LocalDateTime from, LocalDateTime to) {
        List<Consultation> items;

        // Build query based on filters
        if (patientId != null && status != null) {
            if (from != null && to != null) {
                items = consultationRepository.findByPatientIdAndStatusAndDateTimeBetween(patientId, status, from, to);
            } else {
                items = consultationRepository.findByPatientIdAndStatus(patientId, status);
            }
        } else if (patientId != null) {
            if (from != null && to != null) {
                items = consultationRepository.findByPatientIdAndDateTimeBetween(patientId, from, to);
            } else {
                items = consultationRepository.findByPatientId(patientId);
            }
        } else if (status != null) {
            if (from != null && to != null) {
                items = consultationRepository.findByStatusAndDateTimeBetween(status, from, to);
            } else {
                items = consultationRepository.findByStatus(status);
            }
        } else if (from != null && to != null) {
            items = consultationRepository.findByDateTimeBetween(from, to);
        } else {
            items = consultationRepository.findAll();
        }

        // Exclude archived by default unless explicitly requested
        if (status != ConsultationStatus.ARCHIVED) {
            items = items.stream()
                    .filter(item -> item.getStatus() != ConsultationStatus.ARCHIVED)
                    .toList();
        }

        // Load patient details for all consultations
        Map<Long, PatientSummary> patientMap = patientDirectoryClient.getPatientsByIds(
                items.stream().map(Consultation::getPatientId).collect(java.util.stream.Collectors.toSet())
        );

        return items.stream()
                .map(item -> mapToResponse(item, patientMap.get(item.getPatientId())))
                .toList();
    }

    private ConsultationResponse mapToResponse(Consultation c) {
        return mapToResponse(c, resolvePatientName(c.getPatientId()));
    }

    private ConsultationResponse mapToResponse(Consultation c, PatientSummary patient) {
        return mapToResponse(c, formatPatientName(patient));
    }

    private ConsultationResponse mapToResponse(Consultation c, String patientName) {
        return ConsultationResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .patientName(patientName)
                .doctorId(c.getDoctorId())
                .dateTime(c.getDateTime())
                .appointmentId(c.getAppointmentId())
                .startedAt(c.getStartedAt())
                .status(c.getStatus())
                .archivedAt(c.getArchivedAt())
                .archivedBy(c.getArchivedBy())
                .build();
    }

    private String resolvePatientName(Long patientId) {
        if (patientId == null) {
            return null;
        }
        Map<Long, PatientSummary> map = patientDirectoryClient.getPatientsByIds(List.of(patientId));
        return formatPatientName(map.get(patientId));
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
