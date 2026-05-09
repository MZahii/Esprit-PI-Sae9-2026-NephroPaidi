package tn.esprit.spring.clinicalservice.hospitalization.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OpsHospitalizationTaskResponse {

    private UUID id;
    private HospitalizationTaskType type;
    private String title;
    private String instructions;
    private HospitalizationMeasurementKind measurementKind;
    private String expectedUnit;
    private Integer displayOrder;
    private String status;
    private String latestNote;
    private BigDecimal latestNumericValue;
    private String latestTextValue;
    private String latestUnit;
    private String lastUpdatedByNurseId;
    private String lastUpdatedByNurseUsername;
    private LocalDateTime lastUpdatedAt;
    private List<OpsHospitalizationTaskExecutionResponse> executions;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalizationTaskType getType() { return type; }
    public void setType(HospitalizationTaskType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public HospitalizationMeasurementKind getMeasurementKind() { return measurementKind; }
    public void setMeasurementKind(HospitalizationMeasurementKind measurementKind) { this.measurementKind = measurementKind; }
    public String getExpectedUnit() { return expectedUnit; }
    public void setExpectedUnit(String expectedUnit) { this.expectedUnit = expectedUnit; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLatestNote() { return latestNote; }
    public void setLatestNote(String latestNote) { this.latestNote = latestNote; }
    public BigDecimal getLatestNumericValue() { return latestNumericValue; }
    public void setLatestNumericValue(BigDecimal latestNumericValue) { this.latestNumericValue = latestNumericValue; }
    public String getLatestTextValue() { return latestTextValue; }
    public void setLatestTextValue(String latestTextValue) { this.latestTextValue = latestTextValue; }
    public String getLatestUnit() { return latestUnit; }
    public void setLatestUnit(String latestUnit) { this.latestUnit = latestUnit; }
    public String getLastUpdatedByNurseId() { return lastUpdatedByNurseId; }
    public void setLastUpdatedByNurseId(String lastUpdatedByNurseId) { this.lastUpdatedByNurseId = lastUpdatedByNurseId; }
    public String getLastUpdatedByNurseUsername() { return lastUpdatedByNurseUsername; }
    public void setLastUpdatedByNurseUsername(String lastUpdatedByNurseUsername) { this.lastUpdatedByNurseUsername = lastUpdatedByNurseUsername; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public List<OpsHospitalizationTaskExecutionResponse> getExecutions() { return executions; }
    public void setExecutions(List<OpsHospitalizationTaskExecutionResponse> executions) { this.executions = executions; }
}