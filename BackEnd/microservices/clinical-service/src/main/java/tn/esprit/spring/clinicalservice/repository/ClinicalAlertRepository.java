package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.ClinicalAlert;
import tn.esprit.spring.clinicalservice.enums.AlertSeverity;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ClinicalAlert
 */
@Repository
public interface ClinicalAlertRepository extends JpaRepository<ClinicalAlert, UUID> {
    
    /**
     * Find active (unresolved) alerts for a patient
     */
    List<ClinicalAlert> findByPatientIdAndResolvedFalseOrderByCreatedAtDesc(UUID patientId);
    
    /**
     * Find urgent/warning alerts for a patient
     */
    List<ClinicalAlert> findByPatientIdAndSeverityAndResolvedFalseOrderByCreatedAtDesc(UUID patientId, AlertSeverity severity);
}
