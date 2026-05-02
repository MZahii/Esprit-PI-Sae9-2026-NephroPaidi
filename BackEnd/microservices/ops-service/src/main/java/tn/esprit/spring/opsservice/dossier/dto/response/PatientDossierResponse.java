package tn.esprit.spring.opsservice.dossier.dto.response;

import tn.esprit.spring.opsservice.dossier.model.AdmissionPriority;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PatientDossierResponse {

    private UUID id;
    private Long patientId;
    private UUID sourceConsultationId;
    private UUID sourceAppointmentId;
    private UUID hospitalizationRequestId;
    private UUID primaryDoctorId;
    private UUID assignedNurseId;
    private String assignedNurseName;
    private String admissionReason;
    private AdmissionPriority admissionPriority;
    private DossierStatus status;
    private LocalDateTime admittedAt;
    private LocalDateTime expectedDischargeAt;
    private LocalDateTime dischargedAt;
    private boolean archivedToHistory;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public UUID getSourceConsultationId() {
        return sourceConsultationId;
    }

    public void setSourceConsultationId(UUID sourceConsultationId) {
        this.sourceConsultationId = sourceConsultationId;
    }

    public UUID getSourceAppointmentId() {
        return sourceAppointmentId;
    }

    public void setSourceAppointmentId(UUID sourceAppointmentId) {
        this.sourceAppointmentId = sourceAppointmentId;
    }

    public UUID getHospitalizationRequestId() {
        return hospitalizationRequestId;
    }

    public void setHospitalizationRequestId(UUID hospitalizationRequestId) {
        this.hospitalizationRequestId = hospitalizationRequestId;
    }

    public UUID getPrimaryDoctorId() {
        return primaryDoctorId;
    }

    public void setPrimaryDoctorId(UUID primaryDoctorId) {
        this.primaryDoctorId = primaryDoctorId;
    }

    public UUID getAssignedNurseId() {
        return assignedNurseId;
    }

    public void setAssignedNurseId(UUID assignedNurseId) {
        this.assignedNurseId = assignedNurseId;
    }

    public String getAssignedNurseName() {
        return assignedNurseName;
    }

    public void setAssignedNurseName(String assignedNurseName) {
        this.assignedNurseName = assignedNurseName;
    }

    public String getAdmissionReason() {
        return admissionReason;
    }

    public void setAdmissionReason(String admissionReason) {
        this.admissionReason = admissionReason;
    }

    public AdmissionPriority getAdmissionPriority() {
        return admissionPriority;
    }

    public void setAdmissionPriority(AdmissionPriority admissionPriority) {
        this.admissionPriority = admissionPriority;
    }

    public DossierStatus getStatus() {
        return status;
    }

    public void setStatus(DossierStatus status) {
        this.status = status;
    }

    public LocalDateTime getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(LocalDateTime admittedAt) {
        this.admittedAt = admittedAt;
    }

    public LocalDateTime getExpectedDischargeAt() {
        return expectedDischargeAt;
    }

    public void setExpectedDischargeAt(LocalDateTime expectedDischargeAt) {
        this.expectedDischargeAt = expectedDischargeAt;
    }

    public LocalDateTime getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(LocalDateTime dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public boolean isArchivedToHistory() {
        return archivedToHistory;
    }

    public void setArchivedToHistory(boolean archivedToHistory) {
        this.archivedToHistory = archivedToHistory;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
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
}
