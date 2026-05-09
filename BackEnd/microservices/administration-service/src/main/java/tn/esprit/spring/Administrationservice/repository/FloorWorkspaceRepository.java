package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.FloorWorkspace;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;

import java.util.List;

@Repository
public interface FloorWorkspaceRepository extends JpaRepository<FloorWorkspace, Long> {

    List<FloorWorkspace> findByFloorIdOrderByWorkspaceNameAscIdAsc(Long floorId);

    @Query("select coalesce(max(w.sequenceNumber), 0) from FloorWorkspace w where w.floor.id = :floorId and w.workspaceType = :workspaceType")
    Integer findMaxSequenceByFloorIdAndType(@Param("floorId") Long floorId, @Param("workspaceType") WorkspaceType workspaceType);

    List<FloorWorkspace> findByFloorId(Long floorId);

    List<FloorWorkspace> findByFloorIdAndWorkspaceTypeOrderBySequenceNumberAsc(Long floorId, WorkspaceType workspaceType);
}
