package tn.esprit.spring.clinicalservice.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guardian_notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianNotification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "guardian_user_id", nullable = false)
    private Long guardianUserId;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private GuardianNotificationType type;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
