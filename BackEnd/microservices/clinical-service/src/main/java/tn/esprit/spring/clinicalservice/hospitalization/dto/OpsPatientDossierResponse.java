package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class OpsPatientDossierResponse {

    private UUID id;
    private UUID hospitalizationRequestId;
    private UUID assignedNurseId;
    private String status;
    private Long patientId;
    private UUID sourceConsultationId;
    private UUID sourceAppointmentId;
    private AdmissionPriority admissionPriority;
    private LocalDateTime admittedAt;

    public UUID getId() { return id; }
    public UUID getHospitalizationRequestId() { return hospitalizationRequestId; }
    public UUID getAssignedNurseId() { return assignedNurseId; }
    public String getStatus() { return status; }
    public Long getPatientId() { return patientId; }
    public UUID getSourceConsultationId() { return sourceConsultationId; }
    public UUID getSourceAppointmentId() { return sourceAppointmentId; }
    public AdmissionPriority getAdmissionPriority() { return admissionPriority; }
    public LocalDateTime getAdmittedAt() { return admittedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setHospitalizationRequestId(UUID hospitalizationRequestId) { this.hospitalizationRequestId = hospitalizationRequestId; }
    public void setAssignedNurseId(UUID assignedNurseId) { this.assignedNurseId = assignedNurseId; }
    public void setStatus(String status) { this.status = status; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public void setSourceConsultationId(UUID sourceConsultationId) { this.sourceConsultationId = sourceConsultationId; }
    public void setSourceAppointmentId(UUID sourceAppointmentId) { this.sourceAppointmentId = sourceAppointmentId; }
    public void setAdmissionPriority(AdmissionPriority admissionPriority) { this.admissionPriority = admissionPriority; }
    public void setAdmittedAt(LocalDateTime admittedAt) { this.admittedAt = admittedAt; }
}