package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OpsHospitalizationTaskExecutionResponse {

    private UUID id;
    private String status;
    private String nurseKeycloakId;
    private String nurseUsername;
    private String note;
    private BigDecimal numericValue;
    private String textValue;
    private String unit;
    private LocalDateTime recordedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNurseKeycloakId() { return nurseKeycloakId; }
    public void setNurseKeycloakId(String nurseKeycloakId) { this.nurseKeycloakId = nurseKeycloakId; }
    public String getNurseUsername() { return nurseUsername; }
    public void setNurseUsername(String nurseUsername) { this.nurseUsername = nurseUsername; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BigDecimal getNumericValue() { return numericValue; }
    public void setNumericValue(BigDecimal numericValue) { this.numericValue = numericValue; }
    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}