package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.Administrationservice.entity.HospitalEquipment;

import java.util.Optional;

@Repository
public interface HospitalEquipmentRepository extends JpaRepository<HospitalEquipment, Long>, JpaSpecificationExecutor<HospitalEquipment> {
    Optional<HospitalEquipment> findByEquipmentCodeIgnoreCase(String equipmentCode);
    boolean existsByEquipmentCodeIgnoreCase(String equipmentCode);
    Optional<HospitalEquipment> findTopByEquipmentCodeStartingWithOrderByEquipmentCodeDesc(String prefix);
}
