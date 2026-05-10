package tn.esprit.spring.clinicalservice.followup.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentCreateRequest;
import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentResponse;
import tn.esprit.spring.clinicalservice.appointment.service.AppointmentService;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.followup.DoctorFollowUpStatus;
import tn.esprit.spring.clinicalservice.followup.FollowUpOffsetUnit;
import tn.esprit.spring.clinicalservice.followup.dto.*;
import tn.esprit.spring.clinicalservice.followup.entity.DoctorFollowUpRequest;
import tn.esprit.spring.clinicalservice.followup.repository.DoctorFollowUpRequestRepository;
import tn.esprit.spring.clinicalservice.followup.service.DoctorFollowUpRequestService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorFollowUpRequestServiceImpl implements DoctorFollowUpRequestService {

    private final ConsultationRepository consultationRepository;
    private final DoctorFollowUpRequestRepository followUpRepository;
    private final AppointmentService appointmentService;

    @Override
    public DoctorFollowUpResponse createFromConsultation(UUID consultationId, UUID doctorId, DoctorFollowUpCreateRequest request) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));
        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }
        if (doctorId == null || !consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your consultation");
        }

        LocalDate anchor = consultation.getDateTime().toLocalDate();
        LocalDate computed = applyOffset(anchor, request.getOffsetAmount(), request.getOffsetUnit());

        DoctorFollowUpRequest entity = DoctorFollowUpRequest.builder()
                .consultationId(consultationId)
                .patientId(consultation.getPatientId())
                .doctorId(consultation.getDoctorId())
                .anchorDate(anchor)
                .offsetAmount(request.getOffsetAmount())
                .offsetUnit(request.getOffsetUnit())
                .computedReturnDate(computed)
                .status(DoctorFollowUpStatus.PENDING)
                .notes(request.getNotes())
                .build();

        return map(followUpRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorFollowUpResponse> listPending() {
        return followUpRepository.findByStatusOrderByCreatedAtDesc(DoctorFollowUpStatus.PENDING).stream()
                .map(this::map)
                .toList();
    }

    @Override
    public DoctorFollowUpResponse confirm(UUID requestId, DoctorFollowUpConfirmRequest request) {
        DoctorFollowUpRequest fu = followUpRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow-up request not found"));
        if (fu.getStatus() != DoctorFollowUpStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not pending");
        }

        UUID doctorId = request.getDoctorId() != null ? request.getDoctorId() : fu.getDoctorId();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;
        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason()
                : "Doctor follow-up (consultation " + fu.getConsultationId() + ")";

        AppointmentCreateRequest create = AppointmentCreateRequest.builder()
                .patientId(fu.getPatientId())
                .doctorId(doctorId)
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(duration)
                .reason(reason)
                .build();

        AppointmentResponse created = appointmentService.create(create);
        if (created == null || created.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create appointment");
        }

        fu.setStatus(DoctorFollowUpStatus.CONFIRMED);
        return map(followUpRepository.save(fu));
    }

    private static LocalDate applyOffset(LocalDate anchor, int amount, FollowUpOffsetUnit unit) {
        return switch (unit) {
            case DAYS -> anchor.plusDays(amount);
            case WEEKS -> anchor.plusWeeks(amount);
            case MONTHS -> anchor.plusMonths(amount);
        };
    }

    private DoctorFollowUpResponse map(DoctorFollowUpRequest e) {
        return DoctorFollowUpResponse.builder()
                .id(e.getId())
                .consultationId(e.getConsultationId())
                .patientId(e.getPatientId())
                .doctorId(e.getDoctorId())
                .anchorDate(e.getAnchorDate())
                .offsetAmount(e.getOffsetAmount())
                .offsetUnit(e.getOffsetUnit())
                .computedReturnDate(e.getComputedReturnDate())
                .status(e.getStatus())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
