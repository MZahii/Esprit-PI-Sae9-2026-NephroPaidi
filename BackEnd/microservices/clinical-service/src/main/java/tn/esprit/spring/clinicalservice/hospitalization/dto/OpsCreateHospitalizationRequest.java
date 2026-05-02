package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.util.List;
import java.util.UUID;

public class OpsCreateHospitalizationRequest {

    private Long patientId;
    private UUID consultationId;
    private String reason;
    private List<OpsCreateHospitalizationTaskRequest> tasks;

    public OpsCreateHospitalizationRequest() {
    }

    public OpsCreateHospitalizationRequest(Long patientId, UUID consultationId, String reason, List<OpsCreateHospitalizationTaskRequest> tasks) {
        this.patientId = patientId;
        this.consultationId = consultationId;
        this.reason = reason;
        this.tasks = tasks;
    }

    public Long getPatientId() {
        return patientId;
    }

    public UUID getConsultationId() {
        return consultationId;
    }

    public String getReason() {
        return reason;
    }

    public List<OpsCreateHospitalizationTaskRequest> getTasks() {
        return tasks;
    }
}