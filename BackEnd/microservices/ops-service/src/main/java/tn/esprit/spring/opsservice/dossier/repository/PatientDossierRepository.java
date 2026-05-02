package tn.esprit.spring.opsservice.dossier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.spring.opsservice.dossier.entity.PatientDossier;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;

import java.util.UUID;

public interface PatientDossierRepository extends JpaRepository<PatientDossier, UUID>, JpaSpecificationExecutor<PatientDossier> {

    boolean existsByHospitalizationRequestId(UUID hospitalizationRequestId);

    Page<PatientDossier> findByAssignedNurseIdAndStatusIn(UUID assignedNurseId, Iterable<DossierStatus> statuses, Pageable pageable);
}
