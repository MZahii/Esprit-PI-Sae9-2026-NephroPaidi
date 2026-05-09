package tn.esprit.spring.clinicalservice.appointment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.audit.AuditService;
import tn.esprit.spring.clinicalservice.appointment.dto.*;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import tn.esprit.spring.clinicalservice.appointment.repository.AppointmentRepository;
import tn.esprit.spring.clinicalservice.appointment.service.AppointmentService;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.security.ActorResolver;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final PatientDirectoryClient patientDirectoryClient;
    private final AuditService auditService;
    private final ActorResolver actorResolver;

    @Override
    public AppointmentResponse create(AppointmentCreateRequest request) {
        if (request.getScheduledAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt is required");
        }

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;
        validateDuration(duration);
        ensureNoOverlap(request.getDoctorId(), request.getPatientId(), request.getScheduledAt(), duration, null);

        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(duration)
                .reason(request.getReason())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        appointment = appointmentRepository.save(appointment);

        return mapToResponse(appointment);
    }

    @Override
    public AppointmentResponse update(UUID id, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));

        if (appointment.getStatus() == AppointmentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment archived: " + id);
        }

        UUID doctorId = request.getDoctorId() != null ? request.getDoctorId() : appointment.getDoctorId();
        LocalDateTime scheduledAt = request.getScheduledAt() != null ? request.getScheduledAt() : appointment.getScheduledAt();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : appointment.getDurationMinutes();
        validateDuration(duration);

        Long patientId = request.getPatientId() != null ? request.getPatientId() : appointment.getPatientId();
        ensureNoOverlap(doctorId, patientId, scheduledAt, duration, appointment.getId());

        if (request.getPatientId() != null) {
            appointment.setPatientId(request.getPatientId());
        }
        appointment.setDoctorId(doctorId);
        appointment.setScheduledAt(scheduledAt);
        appointment.setDurationMinutes(duration);

        if (request.getReason() != null) {
            appointment.setReason(request.getReason());
        }

        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        appointment = appointmentRepository.save(appointment);

        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));
        if (appointment.getStatus() == AppointmentStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment archived: " + id);
        }
        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(UUID doctorId, Long patientId, AppointmentStatus status, LocalDateTime from, LocalDateTime to) {
        List<Appointment> items = appointmentRepository.findFiltered(doctorId, patientId, status, from, to);
        Map<Long, PatientSummary> patientMap = patientDirectoryClient.getPatientsByIds(
                items.stream().map(Appointment::getPatientId).collect(java.util.stream.Collectors.toSet())
        );
        return items.stream()
                .map(item -> mapToResponse(item, patientMap.get(item.getPatientId())))
                .toList();
    }

    @Override
    public AppointmentResponse cancel(UUID id, AppointmentCancelRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request != null ? request.getReason() : null);

        appointment = appointmentRepository.save(appointment);

        return mapToResponse(appointment);
    }

    @Override
    public StartConsultationResponse startConsultation(UUID id, UUID doctorId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is not active");
        }

        Consultation existing = consultationRepository.findByAppointmentId(appointment.getId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ConsultationStatus.ARCHIVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Consultation archived for this appointment");
            }
            return StartConsultationResponse.builder()
                    .consultationId(existing.getId())
                    .build();
        }

        Consultation consultation = Consultation.builder()
                .appointmentId(appointment.getId())
                .patientId(appointment.getPatientId())
                .doctorId(appointment.getDoctorId())
                .dateTime(appointment.getScheduledAt())
                .status(ConsultationStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        consultation = consultationRepository.save(consultation);

        if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
        }

        return StartConsultationResponse.builder()
                .consultationId(consultation.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentAvailabilityResponse checkAvailability(UUID doctorId, LocalDateTime from, LocalDateTime to) {
        if (doctorId == null || from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctorId, from, and to are required");
        }
        if (!to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be after from");
        }

        boolean conflict = appointmentRepository.existsOverlapping(doctorId, null, null, from, to);
        if (conflict) {
            return AppointmentAvailabilityResponse.builder()
                    .available(false)
                    .conflictReason("Doctor already has an appointment in that time slot")
                    .build();
        }

        return AppointmentAvailabilityResponse.builder()
                .available(true)
                .build();
    }

    @Override
    public void delete(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));

        consultationRepository.findByAppointmentId(appointment.getId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment has an active consultation");
        });

        appointment.setStatus(AppointmentStatus.ARCHIVED);
        appointment.setArchivedAt(LocalDateTime.now());
        appointment.setArchivedBy(actorResolver.resolveCurrent().displayName());
        appointmentRepository.save(appointment);
        auditService.record("APPOINTMENT", appointment.getId(), "ARCHIVE", "Archived appointment");
    }

    private void ensureNoOverlap(UUID doctorId, Long patientId, LocalDateTime start, int durationMinutes, UUID excludeId) {
        if (start == null) {
            return;
        }
        LocalDateTime end = start.plusMinutes(durationMinutes);
        boolean doctorConflict = doctorId != null
                && appointmentRepository.existsOverlapping(doctorId, null, excludeId, start, end);
        if (doctorConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor already has an appointment in that time slot");
        }
        boolean patientConflict = patientId != null
                && appointmentRepository.existsOverlapping(null, patientId, excludeId, start, end);
        if (patientConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient already has an appointment in that time slot");
        }
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return mapToResponse(a, resolvePatientName(a.getPatientId()));
    }

    private AppointmentResponse mapToResponse(Appointment a, PatientSummary patient) {
        return mapToResponse(a, formatPatientName(patient));
    }

    private AppointmentResponse mapToResponse(Appointment a, String patientName) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(patientName)
                .doctorId(a.getDoctorId())
                .scheduledAt(a.getScheduledAt())
                .durationMinutes(a.getDurationMinutes())
                .reason(a.getReason())
                .status(a.getStatus())
                .cancellationReason(a.getCancellationReason())
                .archivedAt(a.getArchivedAt())
                .archivedBy(a.getArchivedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private String resolvePatientName(Long patientId) {
        if (patientId == null) {
            return null;
        }
        Map<Long, PatientSummary> map = patientDirectoryClient.getPatientsByIds(Set.of(patientId));
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

    private void validateDuration(int duration) {
        if (duration < 10 || duration > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationMinutes must be between 10 and 180");
        }
    }
}
