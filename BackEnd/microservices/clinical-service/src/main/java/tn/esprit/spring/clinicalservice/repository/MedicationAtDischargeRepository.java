package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.MedicationAtDischarge;
import java.util.List;
import java.util.UUID;

/**
 * Repository for MedicationAtDischarge
 */
@Repository
public interface MedicationAtDischargeRepository extends JpaRepository<MedicationAtDischarge, UUID> {
    
    /**
     * Find all medications for a discharge document
     */
    List<MedicationAtDischarge> findByDischargeId(UUID dischargeId);
}
