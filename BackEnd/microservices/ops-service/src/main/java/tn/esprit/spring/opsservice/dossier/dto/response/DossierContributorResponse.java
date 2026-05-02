package tn.esprit.spring.opsservice.dossier.dto.response;

import tn.esprit.spring.opsservice.dossier.model.ContributorRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class DossierContributorResponse {

    private UUID id;
    private UUID dossierId;
    private UUID contributorId;
    private ContributorRole contributorRole;
    private boolean active;
    private LocalDateTime addedAt;
    private LocalDateTime removedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDossierId() {
        return dossierId;
    }

    public void setDossierId(UUID dossierId) {
        this.dossierId = dossierId;
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }
}
