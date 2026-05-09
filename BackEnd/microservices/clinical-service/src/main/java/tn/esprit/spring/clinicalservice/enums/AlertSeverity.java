package tn.esprit.spring.clinicalservice.enums;

/**
 * Alert severity classification
 */
public enum AlertSeverity {
    ROUTINE("Routine - informational, no action required"),
    WARNING("Warning - monitor closely, may require action"),
    URGENT("Urgent - requires immediate action");
    
    private final String description;
    
    AlertSeverity(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
