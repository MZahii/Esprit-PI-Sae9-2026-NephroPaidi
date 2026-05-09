package tn.esprit.spring.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.userservice.entity.UserAuditLog;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
    List<UserAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserAuditLog> findTop500ByOrderByCreatedAtDesc();
}
