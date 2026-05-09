package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.Administrationservice.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByOrderByCreatedAtDesc();
    List<Notification> findTop100ByTargetUserIdOrTargetUserIdIsNullOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findTop50ByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findTop50ByTargetUserIdIsNullOrderByCreatedAtDesc();
    List<Notification> findByTargetUserId(Long targetUserId);
}
