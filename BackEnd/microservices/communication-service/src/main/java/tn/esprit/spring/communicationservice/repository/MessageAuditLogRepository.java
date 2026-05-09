package tn.esprit.spring.communicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.domain.entity.MessageAuditLog;
import tn.esprit.spring.communicationservice.domain.enums.MessageAuditAction;

import java.util.List;
import java.util.UUID;

public interface MessageAuditLogRepository extends JpaRepository<MessageAuditLog, UUID> {
    List<MessageAuditLog> findByMessageIdOrderByCreatedAtAsc(UUID messageId);

    boolean existsByMessageIdAndAction(UUID messageId, MessageAuditAction action);
}
