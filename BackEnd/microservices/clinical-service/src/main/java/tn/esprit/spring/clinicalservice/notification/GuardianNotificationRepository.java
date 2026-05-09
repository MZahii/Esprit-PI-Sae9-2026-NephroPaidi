package tn.esprit.spring.clinicalservice.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianNotificationRepository extends JpaRepository<GuardianNotification, UUID> {
    List<GuardianNotification> findByGuardianUserIdOrderByCreatedAtDesc(Long guardianUserId);
}
