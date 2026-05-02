package tn.esprit.spring.opsservice.dossier.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.dossier.dto.request.AddDossierContributorRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierContributorResponse;
import tn.esprit.spring.opsservice.dossier.entity.DossierContributor;
import tn.esprit.spring.opsservice.dossier.repository.DossierContributorRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DossierContributorService {

    private final DossierService dossierService;
    private final DossierContributorRepository dossierContributorRepository;
    private final DossierMapper dossierMapper;

    public DossierContributorService(
            DossierService dossierService,
            DossierContributorRepository dossierContributorRepository,
            DossierMapper dossierMapper
    ) {
        this.dossierService = dossierService;
        this.dossierContributorRepository = dossierContributorRepository;
        this.dossierMapper = dossierMapper;
    }

    @Transactional
    public DossierContributorResponse addContributor(UUID dossierId, AddDossierContributorRequest request, UUID addedBy) {
        dossierService.findDossierOrThrow(dossierId);
        boolean exists = dossierContributorRepository.existsByDossierIdAndContributorIdAndContributorRoleAndActiveTrue(
                dossierId,
                request.getContributorId(),
                request.getContributorRole()
        );
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contributor already active on dossier");
        }

        DossierContributor contributor = new DossierContributor();
        contributor.setDossierId(dossierId);
        contributor.setContributorId(request.getContributorId());
        contributor.setContributorRole(request.getContributorRole());
        contributor.setActive(true);
        contributor.setAddedAt(LocalDateTime.now());
        contributor.setAddedBy(addedBy);

        return dossierMapper.toContributorResponse(dossierContributorRepository.save(contributor));
    }

    @Transactional
    public void removeContributor(UUID dossierId, UUID contributorId, tn.esprit.spring.opsservice.dossier.model.ContributorRole role) {
        dossierService.findDossierOrThrow(dossierId);
        DossierContributor contributor = dossierContributorRepository
                .findByDossierIdAndContributorIdAndContributorRoleAndActiveTrue(dossierId, contributorId, role)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active contributor not found"));

        contributor.setActive(false);
        contributor.setRemovedAt(LocalDateTime.now());
        dossierContributorRepository.save(contributor);
    }
}
