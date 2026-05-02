package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class OpsCreateDossierRequest {

    private Long patientId;
    private UUID sourceConsultationId;
    private UUID sourceAppointmentId;
    private UUID hospitalizationRequestId;
    private UUID primaryDoctorId;
    private String admissionReason;
    private AdmissionPriority admissionPriority;
    private LocalDateTime expectedDischargeAt;

    public OpsCreateDossierRequest() {
    }

    public OpsCreateDossierRequest(Long patientId, UUID sourceConsultationId, UUID sourceAppointmentId, UUID hospitalizationRequestId, UUID primaryDoctorId, String admissionReason, AdmissionPriority admissionPriority, LocalDateTime expectedDischargeAt) {
        this.patientId = patientId;
        this.sourceConsultationId = sourceConsultationId;
        this.sourceAppointmentId = sourceAppointmentId;
        this.hospitalizationRequestId = hospitalizationRequestId;
        this.primaryDoctorId = primaryDoctorId;
        this.admissionReason = admissionReason;
        this.admissionPriority = admissionPriority;
        this.expectedDischargeAt = expectedDischargeAt;
    }

    public Long getPatientId() { return patientId; }
    public UUID getSourceConsultationId() { return sourceConsultationId; }
    public UUID getSourceAppointmentId() { return sourceAppointmentId; }
    public UUID getHospitalizationRequestId() { return hospitalizationRequestId; }
    public UUID getPrimaryDoctorId() { return primaryDoctorId; }
    public String getAdmissionReason() { return admissionReason; }
    public AdmissionPriority getAdmissionPriority() { return admissionPriority; }
    public LocalDateTime getExpectedDischargeAt() { return expectedDischargeAt; }
}