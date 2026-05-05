package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.EquipmentArchiveLog;

import java.util.List;

@Repository
public interface EquipmentArchiveLogRepository extends JpaRepository<EquipmentArchiveLog, Long> {
    List<EquipmentArchiveLog> findByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);
}
