package tn.esprit.spring.clinicalservice.consultation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationResponse;
import tn.esprit.spring.clinicalservice.consultation.dto.GuardianOutcomeResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.service.GuardianConsultationService;
import tn.esprit.spring.clinicalservice.notification.GuardianNotification;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationResponse;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationService;
import tn.esprit.spring.clinicalservice.security.GuardianIdResolver;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/guardian")
@RequiredArgsConstructor
public class GuardianConsultationController {

    private final GuardianConsultationService guardianConsultationService;
    private final GuardianNotificationService guardianNotificationService;
    private final GuardianIdResolver guardianIdResolver;

    @GetMapping("/consultations")
    public ResponseEntity<List<ConsultationResponse>> listConsultations(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "status", required = false) ConsultationStatus status,
            @RequestHeader(value = "X-Guardian-Id", required = false) Long guardianUserId
    ) {
        Long resolvedGuardianId = requireGuardianId(guardianUserId);
        return ResponseEntity.ok(guardianConsultationService.listConsultations(resolvedGuardianId, patientId, status));
    }

    @GetMapping("/consultations/{id}/outcomes")
    public ResponseEntity<GuardianOutcomeResponse> getOutcomes(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Guardian-Id", required = false) Long guardianUserId,
            Authentication authentication
    ) {
        Long resolvedGuardianId = requireGuardianId(guardianUserId);
        return ResponseEntity.ok(guardianConsultationService.getOutcome(id, resolvedGuardianId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<GuardianNotificationResponse>> listNotifications(
            @RequestHeader(value = "X-Guardian-Id", required = false) Long guardianUserId
    ) {
        Long resolvedGuardianId = requireGuardianId(guardianUserId);
        List<GuardianNotificationResponse> responses = guardianNotificationService.listForGuardian(resolvedGuardianId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<GuardianNotificationResponse> markRead(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Guardian-Id", required = false) Long guardianUserId
    ) {
        Long resolvedGuardianId = requireGuardianId(guardianUserId);
        GuardianNotification updated = guardianNotificationService.markRead(id, resolvedGuardianId);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return ResponseEntity.ok(toResponse(updated));
    }

    private GuardianNotificationResponse toResponse(GuardianNotification notification) {
        return GuardianNotificationResponse.builder()
                .id(notification.getId())
                .consultationId(notification.getConsultationId())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .message(notification.getMessage())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    private Long requireGuardianId(Long guardianUserId) {
        Long resolved = guardianIdResolver.resolve(guardianUserId);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Guardian-Id is required");
        }
        return resolved;
    }
}
