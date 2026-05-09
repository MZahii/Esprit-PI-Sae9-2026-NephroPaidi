package tn.esprit.spring.clinicalservice.hospitalization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HospitalizationTaskRequest {

    @NotNull
    private HospitalizationTaskType type;

    @NotBlank
    @Size(max = 160)
    private String title;

    @Size(max = 1000)
    private String instructions;

    private HospitalizationMeasurementKind measurementKind;

    @Size(max = 32)
    private String expectedUnit;

    private Integer displayOrder;

    public HospitalizationTaskType getType() {
        return type;
    }

    public void setType(HospitalizationTaskType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public HospitalizationMeasurementKind getMeasurementKind() {
        return measurementKind;
    }

    public void setMeasurementKind(HospitalizationMeasurementKind measurementKind) {
        this.measurementKind = measurementKind;
    }

    public String getExpectedUnit() {
        return expectedUnit;
    }

    public void setExpectedUnit(String expectedUnit) {
        this.expectedUnit = expectedUnit;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}