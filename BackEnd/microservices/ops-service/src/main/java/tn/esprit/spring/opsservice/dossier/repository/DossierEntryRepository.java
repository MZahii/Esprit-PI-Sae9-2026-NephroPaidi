package tn.esprit.spring.opsservice.dossier.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.spring.opsservice.dossier.entity.DossierEntry;

import java.util.UUID;

public interface DossierEntryRepository extends JpaRepository<DossierEntry, UUID>, JpaSpecificationExecutor<DossierEntry> {

    Page<DossierEntry> findByDossierId(UUID dossierId, Pageable pageable);
}
