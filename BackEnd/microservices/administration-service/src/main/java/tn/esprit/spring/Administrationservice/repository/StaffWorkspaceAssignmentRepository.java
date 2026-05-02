package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;
import tn.esprit.spring.Administrationservice.entity.StaffWorkspaceAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffWorkspaceAssignmentRepository extends JpaRepository<StaffWorkspaceAssignment, Long> {

    List<StaffWorkspaceAssignment> findByRoleOrderByCreatedAtDesc(StaffPlacementRole role);

    List<StaffWorkspaceAssignment> findByWorkspaceIdAndRole(Long workspaceId, StaffPlacementRole role);

    Optional<StaffWorkspaceAssignment> findByUserIdAndRole(Long userId, StaffPlacementRole role);

    boolean existsByWorkspaceIdAndRole(Long workspaceId, StaffPlacementRole role);

    boolean existsByUserIdAndRole(Long userId, StaffPlacementRole role);

    Optional<StaffWorkspaceAssignment> findByUserIdAndWorkspaceIdAndRole(Long userId, Long workspaceId, StaffPlacementRole role);

    long countByWorkspaceIdAndRole(Long workspaceId, StaffPlacementRole role);
}
