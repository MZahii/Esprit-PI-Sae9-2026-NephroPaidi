package tn.esprit.spring.communicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.domain.entity.QuickReplyTemplate;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;

import java.util.List;
import java.util.UUID;

public interface QuickReplyTemplateRepository extends JpaRepository<QuickReplyTemplate, UUID> {
    List<QuickReplyTemplate> findByStaffRoleAndMessageTypeOrderByNameAsc(StaffRole staffRole, MessageType messageType);

    List<QuickReplyTemplate> findByStaffRoleOrderByNameAsc(StaffRole staffRole);
}
