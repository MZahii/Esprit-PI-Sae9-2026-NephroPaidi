package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.InternalNotificationRequest;
import tn.esprit.spring.Administrationservice.dto.response.AuditLogResponse;
import tn.esprit.spring.Administrationservice.dto.response.NotificationResponse;
import tn.esprit.spring.Administrationservice.entity.Notification;
import tn.esprit.spring.Administrationservice.service.ObservabilityService;

import java.util.List;

/**
 * Observability Controller - Handles notifications and audit trail endpoints
 */
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    /**
     * Get notifications for a user
     * @param userId Optional user ID filter
     * @return List of notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Long userId) {
        List<NotificationResponse> notifications = observabilityService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark notification as read
     * @param notificationId Notification ID
     * @return Success response
     */
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long notificationId) {
        observabilityService.markNotificationAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get contract audit timeline
     * @return List of contract audit logs
     */
    @GetMapping("/contracts/timeline")
    public ResponseEntity<List<AuditLogResponse>> getContractTimeline() {
        List<AuditLogResponse> timeline = observabilityService.getContractAuditTimeline();
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/internal/notifications")
    public ResponseEntity<NotificationResponse> createInternalNotification(@Valid @RequestBody InternalNotificationRequest request) {
        Notification created = observabilityService.createNotification(
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getTargetUserId()
        );
        return ResponseEntity.ok(NotificationResponse.from(created));
    }
}
