package tn.esprit.spring.opsservice.dossier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dossier_contributor",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dossier_contributor_active",
                        columnNames = {"dossier_id", "contributor_id", "contributor_role", "active"}
                )
        }
)
public class DossierContributor {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "dossier_id", nullable = false, columnDefinition = "uuid")
    private UUID dossierId;

    @Column(name = "contributor_id", nullable = false, columnDefinition = "uuid")
    private UUID contributorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contributor_role", nullable = false, length = 16)
    private ContributorRole contributorRole;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "added_by", nullable = false, columnDefinition = "uuid")
    private UUID addedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (!active) {
            active = true;
        }
        if (addedAt == null) {
            addedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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

    public UUID getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UUID addedBy) {
        this.addedBy = addedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
