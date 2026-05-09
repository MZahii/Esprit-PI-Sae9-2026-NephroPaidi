package tn.esprit.spring.communicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.domain.entity.MessageReply;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageReplyRepository extends JpaRepository<MessageReply, UUID> {
    List<MessageReply> findByMessageIdOrderByCreatedAtAsc(UUID messageId);

    boolean existsByMessageIdAndSenderRoleIn(UUID messageId, Collection<SenderRole> senderRoles);
}
