package tn.esprit.spring.communicationservice.staffmessaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.staffmessaging.domain.entity.StaffConversation;

import java.util.Optional;
import java.util.UUID;

public interface StaffConversationRepository extends JpaRepository<StaffConversation, UUID> {
    Optional<StaffConversation> findByDirectConversationKey(String directConversationKey);
}
