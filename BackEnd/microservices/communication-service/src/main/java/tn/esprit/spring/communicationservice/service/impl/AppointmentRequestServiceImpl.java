package tn.esprit.spring.communicationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.communicationservice.domain.entity.AppointmentRequest;
import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;
import tn.esprit.spring.communicationservice.dto.request.ApproveAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.RejectAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;
import tn.esprit.spring.communicationservice.exception.BadRequestException;
import tn.esprit.spring.communicationservice.exception.ResourceNotFoundException;
import tn.esprit.spring.communicationservice.mapper.AppointmentMapper;
import tn.esprit.spring.communicationservice.repository.AppointmentRequestRepository;
import tn.esprit.spring.communicationservice.security.CurrentUserService;
import tn.esprit.spring.communicationservice.service.AppointmentRequestService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentRequestServiceImpl implements AppointmentRequestService {

    private final AppointmentRequestRepository appointmentRequestRepository;
    private final AppointmentMapper appointmentMapper;
    private final CurrentUserService currentUserService;
    private final GuardianPatientResolverService guardianPatientResolverService;

    @Override
    @Transactional
    public AppointmentRequestResponse create(CreateAppointmentRequest request) {
        currentUserService.requireRole("GUARDIAN");
        Instant now = Instant.now();
        Long resolvedPatientId = guardianPatientResolverService.resolvePatientIdForMessage(request.getPatientId());

        if (request.getRequestedDate() != null && !request.getRequestedDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Requested date must be in the future");
        }

        AppointmentRequest entity = new AppointmentRequest();
        entity.setPatientId(resolvedPatientId);
        entity.setGuardianKeycloakId(currentUserService.getCurrentUserSub());
        entity.setRequestedDate(request.getRequestedDate());
        entity.setReason(request.getReason().trim());
        entity.setStatus(AppointmentStatus.REQUESTED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return appointmentMapper.toResponse(appointmentRequestRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentRequestResponse> myRequests() {
        currentUserService.requireRole("GUARDIAN");
        String sub = currentUserService.getCurrentUserSub();

        return appointmentRequestRepository.findByGuardianKeycloakIdOrderByCreatedAtDesc(sub)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentRequestResponse> listRequests(AppointmentStatus status) {
        currentUserService.requireRole("RECEPTIONIST");

        return appointmentRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> status == null || item.getStatus() == status)
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AppointmentRequestResponse approve(UUID id, ApproveAppointmentRequest request) {
        currentUserService.requireRole("RECEPTIONIST");
        AppointmentRequest entity = getOrThrow(id);

        if (entity.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED appointments can be approved");
        }

        if (request.getScheduledDate() == null) {
            throw new BadRequestException("Scheduled date is required");
        }

        if (!request.getScheduledDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Scheduled date must be in the future");
        }

        entity.setScheduledDate(request.getScheduledDate());
        entity.setReceptionistNotes(trimOrNull(request.getReceptionistNotes()));
        entity.setStatus(AppointmentStatus.APPROVED);
        entity.setUpdatedAt(Instant.now());

        return appointmentMapper.toResponse(appointmentRequestRepository.save(entity));
    }

    @Override
    @Transactional
    public AppointmentRequestResponse reject(UUID id, RejectAppointmentRequest request) {
        currentUserService.requireRole("RECEPTIONIST");
        AppointmentRequest entity = getOrThrow(id);

        if (entity.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED appointments can be rejected");
        }

        entity.setReceptionistNotes(trimOrNull(request.getReceptionistNotes()));
        entity.setStatus(AppointmentStatus.REJECTED);
        entity.setUpdatedAt(Instant.now());

        return appointmentMapper.toResponse(appointmentRequestRepository.save(entity));
    }

    @Override
    @Transactional
    public AppointmentRequestResponse cancel(UUID id) {
        currentUserService.requireRole("GUARDIAN");
        String sub = currentUserService.getCurrentUserSub();
        AppointmentRequest entity = getOrThrow(id);

        if (!sub.equals(entity.getGuardianKeycloakId())) {
            throw new AccessDeniedException("Guardian can only cancel own requests");
        }

        if (entity.getStatus() != AppointmentStatus.REQUESTED && entity.getStatus() != AppointmentStatus.APPROVED) {
            throw new BadRequestException("Only REQUESTED or APPROVED appointments can be cancelled");
        }

        entity.setStatus(AppointmentStatus.CANCELLED);
        entity.setUpdatedAt(Instant.now());

        return appointmentMapper.toResponse(appointmentRequestRepository.save(entity));
    }

    private AppointmentRequest getOrThrow(UUID id) {
        return appointmentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment request not found: " + id));
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
