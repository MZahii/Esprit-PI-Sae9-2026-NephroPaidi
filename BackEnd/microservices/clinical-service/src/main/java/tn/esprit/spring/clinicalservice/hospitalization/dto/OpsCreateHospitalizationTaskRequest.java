package tn.esprit.spring.clinicalservice.hospitalization.dto;

public class OpsCreateHospitalizationTaskRequest {

    private HospitalizationTaskType type;
    private String title;
    private String instructions;
    private HospitalizationMeasurementKind measurementKind;
    private String expectedUnit;
    private Integer displayOrder;

    public OpsCreateHospitalizationTaskRequest() {
    }

    public OpsCreateHospitalizationTaskRequest(HospitalizationTaskType type, String title, String instructions, HospitalizationMeasurementKind measurementKind, String expectedUnit, Integer displayOrder) {
        this.type = type;
        this.title = title;
        this.instructions = instructions;
        this.measurementKind = measurementKind;
        this.expectedUnit = expectedUnit;
        this.displayOrder = displayOrder;
    }

    public HospitalizationTaskType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getInstructions() {
        return instructions;
    }

    public HospitalizationMeasurementKind getMeasurementKind() {
        return measurementKind;
    }

    public String getExpectedUnit() {
        return expectedUnit;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}