package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.HospitalFloor;

import java.util.List;

@Repository
public interface HospitalFloorRepository extends JpaRepository<HospitalFloor, Long> {

    List<HospitalFloor> findAllByOrderByFloorOrderAsc();

    @Query("select distinct f from HospitalFloor f left join fetch f.workspaces order by f.floorOrder asc")
    List<HospitalFloor> findAllWithWorkspacesOrdered();
}
