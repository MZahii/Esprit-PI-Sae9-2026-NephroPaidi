package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OpsHospitalizationCaseResponse {

    private UUID id;
    private Long patientId;
    private UUID consultationId;
    private String doctorKeycloakId;
    private String doctorUsername;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OpsHospitalizationTaskResponse> tasks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public UUID getConsultationId() { return consultationId; }
    public void setConsultationId(UUID consultationId) { this.consultationId = consultationId; }
    public String getDoctorKeycloakId() { return doctorKeycloakId; }
    public void setDoctorKeycloakId(String doctorKeycloakId) { this.doctorKeycloakId = doctorKeycloakId; }
    public String getDoctorUsername() { return doctorUsername; }
    public void setDoctorUsername(String doctorUsername) { this.doctorUsername = doctorUsername; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<OpsHospitalizationTaskResponse> getTasks() { return tasks; }
    public void setTasks(List<OpsHospitalizationTaskResponse> tasks) { this.tasks = tasks; }
}