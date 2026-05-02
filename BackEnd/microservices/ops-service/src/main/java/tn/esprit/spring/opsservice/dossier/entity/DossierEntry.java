package tn.esprit.spring.opsservice.dossier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;
import tn.esprit.spring.opsservice.dossier.model.SignatureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dossier_entry")
public class DossierEntry {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "dossier_id", nullable = false, columnDefinition = "uuid")
    private UUID dossierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 24)
    private DossierEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 16)
    private ActorRole actorRole;

    @Column(name = "actor_id", nullable = false, columnDefinition = "uuid")
    private UUID actorId;

    @Column(name = "actor_display_name", nullable = false, length = 120)
    private String actorDisplayName;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "medication_id", columnDefinition = "uuid")
    private UUID medicationId;

    @Column(name = "medication_name", length = 180)
    private String medicationName;

    @Column(name = "dose_value", precision = 10, scale = 2)
    private BigDecimal doseValue;

    @Column(name = "dose_unit", length = 24)
    private String doseUnit;

    @Column(name = "route", length = 32)
    private String route;

    @Column(name = "patient_condition", length = 64)
    private String patientCondition;

    @Column(name = "vitals_json", columnDefinition = "text")
    private String vitalsJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "requires_signature", nullable = false)
    private boolean requiresSignature;

    @Column(name = "signed", nullable = false)
    private boolean signed;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", length = 20)
    private SignatureType signatureType;

    @Column(name = "signature_hash", length = 128)
    private String signatureHash;

    @Column(name = "previous_entry_id", columnDefinition = "uuid")
    private UUID previousEntryId;

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
        if (occurredAt == null) {
            occurredAt = now;
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

    public UUID getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(UUID medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public BigDecimal getDoseValue() {
        return doseValue;
    }

    public void setDoseValue(BigDecimal doseValue) {
        this.doseValue = doseValue;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getPatientCondition() {
        return patientCondition;
    }

    public void setPatientCondition(String patientCondition) {
        this.patientCondition = patientCondition;
    }

    public String getVitalsJson() {
        return vitalsJson;
    }

    public void setVitalsJson(String vitalsJson) {
        this.vitalsJson = vitalsJson;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public void setSignatureHash(String signatureHash) {
        this.signatureHash = signatureHash;
    }

    public UUID getPreviousEntryId() {
        return previousEntryId;
    }

    public void setPreviousEntryId(UUID previousEntryId) {
        this.previousEntryId = previousEntryId;
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
