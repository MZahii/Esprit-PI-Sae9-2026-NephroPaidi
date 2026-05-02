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
import tn.esprit.spring.opsservice.dossier.model.AdmissionPriority;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "patient_dossier",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_patient_dossier_hospitalization_request", columnNames = "hospitalization_request_id")
        }
)
public class PatientDossier {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "source_consultation_id", nullable = false, columnDefinition = "uuid")
    private UUID sourceConsultationId;

    @Column(name = "source_appointment_id", nullable = false, columnDefinition = "uuid")
    private UUID sourceAppointmentId;

    @Column(name = "hospitalization_request_id", nullable = false, columnDefinition = "uuid")
    private UUID hospitalizationRequestId;

    @Column(name = "primary_doctor_id", nullable = false, columnDefinition = "uuid")
    private UUID primaryDoctorId;

    @Column(name = "assigned_nurse_id", columnDefinition = "uuid")
    private UUID assignedNurseId;

    @Column(name = "admission_reason", nullable = false, columnDefinition = "text")
    private String admissionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_priority", nullable = false, length = 20)
    private AdmissionPriority admissionPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private DossierStatus status;

    @Column(name = "admitted_at", nullable = false)
    private LocalDateTime admittedAt;

    @Column(name = "expected_discharge_at")
    private LocalDateTime expectedDischargeAt;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt;

    @Column(name = "discharge_summary", columnDefinition = "text")
    private String dischargeSummary;

    @Column(name = "archived_to_history", nullable = false)
    private boolean archivedToHistory;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

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
        if (status == null) {
            status = DossierStatus.ACTIVE;
        }
        if (admittedAt == null) {
            admittedAt = now;
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

    public String getDischargeSummary() {
        return dischargeSummary;
    }

    public void setDischargeSummary(String dischargeSummary) {
        this.dischargeSummary = dischargeSummary;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
