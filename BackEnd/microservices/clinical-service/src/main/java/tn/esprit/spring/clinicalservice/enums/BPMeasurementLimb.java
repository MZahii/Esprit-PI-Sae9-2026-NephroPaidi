package tn.esprit.spring.clinicalservice.enums;

/**
 * Anatomical site for blood pressure measurement
 */
public enum BPMeasurementLimb {
    RIGHT_ARM("Right arm"),
    LEFT_ARM("Left arm"),
    RIGHT_LEG("Right leg"),
    LEFT_LEG("Left leg");
    
    private final String description;
    
    BPMeasurementLimb(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
