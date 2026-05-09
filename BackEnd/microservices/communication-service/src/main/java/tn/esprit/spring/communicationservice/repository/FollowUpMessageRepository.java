package tn.esprit.spring.communicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.spring.communicationservice.domain.entity.FollowUpMessage;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;

import java.util.List;
import java.util.UUID;

public interface FollowUpMessageRepository extends JpaRepository<FollowUpMessage, UUID>, JpaSpecificationExecutor<FollowUpMessage> {
    List<FollowUpMessage> findByGuardianKeycloakIdOrderByCreatedAtDesc(String guardianKeycloakId);

    List<FollowUpMessage> findByPriorityAndStatusNot(PriorityLevel priority, MessageStatus status);
}
