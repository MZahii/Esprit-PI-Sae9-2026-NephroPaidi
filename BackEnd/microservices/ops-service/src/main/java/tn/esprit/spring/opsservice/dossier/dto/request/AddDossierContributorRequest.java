package tn.esprit.spring.opsservice.dossier.dto.request;

import jakarta.validation.constraints.NotNull;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;

import java.util.UUID;

public class AddDossierContributorRequest {

    @NotNull
    private UUID contributorId;

    @NotNull
    private ContributorRole contributorRole;

    public UUID getContributorId() {
        return contributorId;
    }

    public void setContributorId(UUID contributorId) {
        this.contributorId = contributorId;
    }

    public ContributorRole getContributorRole() {
        return contributorRole;
    }

    public void setContributorRole(ContributorRole contributorRole) {
        this.contributorRole = contributorRole;
    }
}
