package tn.esprit.spring.clinicalservice.hospitalization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class HospitalizationCreateRequest {

    @NotBlank
    private String admissionReason;

    @NotNull
    private AdmissionPriority admissionPriority;

    private LocalDateTime expectedDischargeAt;

    @Valid
    private List<HospitalizationTaskRequest> tasks;

    public HospitalizationCreateRequest() {
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

    public List<HospitalizationTaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<HospitalizationTaskRequest> tasks) {
        this.tasks = tasks;
    }

    public List<HospitalizationTaskRequest> safeTasks() {
        return tasks == null ? new ArrayList<>() : tasks;
    }
}