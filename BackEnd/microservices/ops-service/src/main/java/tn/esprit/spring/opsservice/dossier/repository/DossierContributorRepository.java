package tn.esprit.spring.opsservice.dossier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.opsservice.dossier.entity.DossierContributor;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;

import java.util.Optional;
import java.util.UUID;

public interface DossierContributorRepository extends JpaRepository<DossierContributor, UUID> {

    boolean existsByDossierIdAndContributorIdAndContributorRoleAndActiveTrue(
            UUID dossierId,
            UUID contributorId,
            ContributorRole contributorRole
    );

    Optional<DossierContributor> findByDossierIdAndContributorIdAndContributorRoleAndActiveTrue(
            UUID dossierId,
            UUID contributorId,
            ContributorRole contributorRole
    );
}
