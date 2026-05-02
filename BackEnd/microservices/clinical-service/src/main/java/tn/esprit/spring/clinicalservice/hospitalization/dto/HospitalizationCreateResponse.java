package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalizationCreateResponse {

    private UUID hospitalizationId;
    private UUID consultationId;
    private Long patientId;
    private String status;
    private String reason;
    private int taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getHospitalizationId() {
        return hospitalizationId;
    }

    public void setHospitalizationId(UUID hospitalizationId) {
        this.hospitalizationId = hospitalizationId;
    }

    public UUID getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(UUID consultationId) {
        this.consultationId = consultationId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
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