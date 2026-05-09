package tn.esprit.spring.clinicalservice.enums;

/**
 * Renal Replacement Therapy (RRT) types for end-stage renal disease
 */
public enum RRTType {
    HEMODIALYSIS("Hemodialysis (in-center or home-based)"),
    PERITONEAL_DIALYSIS("Peritoneal Dialysis (CAPD or APD)"),
    CRRT("Continuous Renal Replacement Therapy (acute setting)"),
    TRANSPLANT("Renal Transplant");

    private final String description;

    RRTType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
