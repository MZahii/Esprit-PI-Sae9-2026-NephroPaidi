package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.EquipmentPlacement;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentPlacementRepository extends JpaRepository<EquipmentPlacement, Long> {
    Optional<EquipmentPlacement> findByEquipmentIdAndActiveTrue(Long equipmentId);
    List<EquipmentPlacement> findByWorkspaceIdAndActiveTrueOrderByPlacedAtAsc(Long workspaceId);
    List<EquipmentPlacement> findByWorkspaceId(Long workspaceId);
    List<EquipmentPlacement> findByWorkspaceFloorIdAndActiveTrue(Long floorId);
    List<EquipmentPlacement> findByWorkspaceFloorId(Long floorId);
    List<EquipmentPlacement> findByActiveTrue();
    List<EquipmentPlacement> findByEquipmentIdInAndActiveTrue(List<Long> equipmentIds);
}
