package tn.esprit.spring.opsservice.dossier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class CreateDossierEntryRequest {

    @NotNull
    private DossierEntryType entryType;

    @NotBlank
    private String title;

    private String details;
    private UUID medicationId;
    private String medicationName;
    private BigDecimal doseValue;
    private String doseUnit;
    private String route;
    private String patientCondition;
    private Map<String, Object> vitals;
    private LocalDateTime occurredAt;
    private Boolean requiresSignature;

    public DossierEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(DossierEntryType entryType) {
        this.entryType = entryType;
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

    public Map<String, Object> getVitals() {
        return vitals;
    }

    public void setVitals(Map<String, Object> vitals) {
        this.vitals = vitals;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Boolean getRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(Boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }
}
