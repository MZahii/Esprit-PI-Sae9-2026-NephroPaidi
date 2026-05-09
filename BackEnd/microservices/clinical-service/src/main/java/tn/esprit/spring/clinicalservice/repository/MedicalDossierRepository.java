package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.MedicalDossierEntry;
import tn.esprit.spring.clinicalservice.enums.EntryType;
import java.util.List;
import java.util.UUID;

/**
 * Repository for MedicalDossierEntry persistence
 */
@Repository
public interface MedicalDossierRepository extends JpaRepository<MedicalDossierEntry, UUID> {
    
    /**
     * Find all dossier entries for a patient, ordered by creation date descending
     */
    List<MedicalDossierEntry> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    
    /**
     * Find dossier entries filtered by type for a patient
     */
    List<MedicalDossierEntry> findByPatientIdAndEntryTypeOrderByCreatedAtDesc(UUID patientId, EntryType entryType);
}
