package tn.esprit.spring.communicationservice.staffmessaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversationParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffConversationParticipantRepository extends JpaRepository<StaffConversationParticipant, UUID> {

    @Query("""
            select participant
            from StaffConversationParticipant participant
            join fetch participant.conversation conversation
            where participant.userId = :userId and participant.archived = false
            order by coalesce(conversation.lastMessageAt, conversation.updatedAt) desc, conversation.updatedAt desc
            """)
    List<StaffConversationParticipant> findActiveByUserIdOrderByRecentActivity(@Param("userId") String userId);

    Optional<StaffConversationParticipant> findByConversation_IdAndUserId(UUID conversationId, String userId);

    List<StaffConversationParticipant> findByConversation_IdOrderByJoinedAtAsc(UUID conversationId);
}
