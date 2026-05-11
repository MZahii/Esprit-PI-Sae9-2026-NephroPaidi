package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.HospitalizationRecord;
import java.util.List;
import java.util.UUID;

/**
 * Repository for HospitalizationRecord
 */
@Repository
public interface HospitalizationRecordRepository extends JpaRepository<HospitalizationRecord, UUID> {
    
    /**
     * Find all hospitalizations for a patient, ordered by admission date descending
     */
    List<HospitalizationRecord> findByPatientIdOrderByAdmissionDateDesc(UUID patientId);
}
