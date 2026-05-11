package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ConsultationRecord
 */
@Repository
public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, UUID> {
    
    /**
     * Find all consultations for a patient, ordered by date descending
     */
    List<ConsultationRecord> findByPatientIdOrderByConsultationDateDesc(UUID patientId);
}
