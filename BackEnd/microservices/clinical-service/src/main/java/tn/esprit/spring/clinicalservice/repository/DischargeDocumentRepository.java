package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;
import java.util.List;
import java.util.UUID;

/**
 * Repository for DischargeDocument
 */
@Repository
public interface DischargeDocumentRepository extends JpaRepository<DischargeDocument, UUID> {
    
    /**
     * Find all discharge documents for a patient, ordered by date descending
     */
    List<DischargeDocument> findByPatientIdOrderByDischargeDateDesc(UUID patientId);
}
