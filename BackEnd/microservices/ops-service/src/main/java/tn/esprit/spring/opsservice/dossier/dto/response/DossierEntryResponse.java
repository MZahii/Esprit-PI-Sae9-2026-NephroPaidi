package tn.esprit.spring.opsservice.dossier.dto.response;

import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;

import java.time.LocalDateTime;
import java.util.UUID;

public class DossierEntryResponse {

    private UUID id;
    private UUID dossierId;
    private DossierEntryType entryType;
    private ActorRole actorRole;
    private UUID actorId;
    private String actorDisplayName;
    private String title;
    private String details;
    private boolean signed;
    private boolean requiresSignature;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;

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

    public DossierEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(DossierEntryType entryType) {
        this.entryType = entryType;
    }

    public ActorRole getActorRole() {
        return actorRole;
    }

    public void setActorRole(ActorRole actorRole) {
        this.actorRole = actorRole;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public void setActorDisplayName(String actorDisplayName) {
        this.actorDisplayName = actorDisplayName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean isRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
