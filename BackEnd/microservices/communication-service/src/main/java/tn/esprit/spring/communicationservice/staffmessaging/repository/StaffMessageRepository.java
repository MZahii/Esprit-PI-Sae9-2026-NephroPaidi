package tn.esprit.spring.communicationservice.staffmessaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffMessageRepository extends JpaRepository<StaffMessage, UUID> {
    List<StaffMessage> findByConversation_IdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID conversationId);

    Optional<StaffMessage> findTopByConversation_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID conversationId);

    long countByConversation_IdAndSenderIdNotAndDeletedAtIsNull(UUID conversationId, String senderId);

    long countByConversation_IdAndSenderIdNotAndCreatedAtAfterAndDeletedAtIsNull(UUID conversationId, String senderId, Instant createdAt);
}
