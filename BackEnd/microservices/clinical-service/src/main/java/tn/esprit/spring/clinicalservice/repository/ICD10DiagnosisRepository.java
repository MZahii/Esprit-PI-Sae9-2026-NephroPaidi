package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.ICD10Diagnosis;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ICD10Diagnosis
 */
@Repository
public interface ICD10DiagnosisRepository extends JpaRepository<ICD10Diagnosis, UUID> {
    
    /**
     * Find all diagnoses for a consultation
     */
    List<ICD10Diagnosis> findByConsultationId(UUID consultationId);
}
