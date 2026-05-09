package tn.esprit.spring.opsservice.dossier.dto.response;

import tn.esprit.spring.opsservice.dossier.model.AdmissionPriority;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PatientDossierSummaryResponse {

    private UUID id;
    private Long patientId;
    private DossierStatus status;
    private AdmissionPriority admissionPriority;
    private UUID assignedNurseId;
    private LocalDateTime admittedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public DossierStatus getStatus() {
        return status;
    }

    public void setStatus(DossierStatus status) {
        this.status = status;
    }

    public AdmissionPriority getAdmissionPriority() {
        return admissionPriority;
    }

    public void setAdmissionPriority(AdmissionPriority admissionPriority) {
        this.admissionPriority = admissionPriority;
    }

    public UUID getAssignedNurseId() {
        return assignedNurseId;
    }

    public void setAssignedNurseId(UUID assignedNurseId) {
        this.assignedNurseId = assignedNurseId;
    }

    public LocalDateTime getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(LocalDateTime admittedAt) {
        this.admittedAt = admittedAt;
    }
}
