package tn.esprit.spring.opsservice.dossier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.spring.opsservice.dossier.model.AdmissionPriority;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreatePatientDossierRequest {

    @NotNull
    private Long patientId;

    @NotNull
    private UUID sourceConsultationId;

    @NotNull
    private UUID sourceAppointmentId;

    @NotNull
    private UUID hospitalizationRequestId;

    @NotNull
    private UUID primaryDoctorId;

    @NotBlank
    private String admissionReason;

    @NotNull
    private AdmissionPriority admissionPriority;

    private LocalDateTime expectedDischargeAt;

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

    public LocalDateTime getExpectedDischargeAt() {
        return expectedDischargeAt;
    }

    public void setExpectedDischargeAt(LocalDateTime expectedDischargeAt) {
        this.expectedDischargeAt = expectedDischargeAt;
    }
}
