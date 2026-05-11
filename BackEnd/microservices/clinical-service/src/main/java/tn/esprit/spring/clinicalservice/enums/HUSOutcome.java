package tn.esprit.spring.clinicalservice.enums;

/**
 * Clinical outcomes of Hemolytic Uremic Syndrome
 */
public enum HUSOutcome {
    RECOVERY("Complete recovery with normal renal function"),
    RESIDUAL_HTA("Recovery with residual hypertension"),
    CHRONIC_RENAL_FAILURE("Progression to chronic renal failure requiring RRT"),
    FATAL("Fatal outcome despite supportive care");

    private final String description;

    HUSOutcome(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
