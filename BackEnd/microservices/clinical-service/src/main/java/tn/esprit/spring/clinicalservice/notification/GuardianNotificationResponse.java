package tn.esprit.spring.clinicalservice.notification;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class GuardianNotificationResponse {
    private UUID id;
    private UUID consultationId;
    private String type;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
