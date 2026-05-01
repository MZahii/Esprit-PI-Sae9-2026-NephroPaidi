package tn.esprit.spring.clinicalservice.discharge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.discharge.entity.FollowUpItem;

import java.util.List;
import java.util.UUID;

public interface FollowUpItemRepository extends JpaRepository<FollowUpItem, UUID> {
    List<FollowUpItem> findByFollowUpIdOrderByCreatedAtDesc(UUID followUpId);
    List<FollowUpItem> findByFollowUpIdAndStatus(UUID followUpId, FollowUpItem.ItemStatus status);
}
