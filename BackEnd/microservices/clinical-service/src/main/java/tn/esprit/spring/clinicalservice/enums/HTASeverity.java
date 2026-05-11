package tn.esprit.spring.clinicalservice.enums;

/**
 * Classification of hypertension severity in children
 * Adapted for pediatric nephrology management
 */
public enum HTASeverity {
    NORMAL("Normal: <90th percentile for age/sex/height"),
    HIGH_NORMAL("High-normal: 90th-<95th percentile"),
    MODERATE("Moderate HTA: 95th-<99th percentile + 5 mmHg"),
    SEVERE("Severe HTA: ≥99th percentile + 5 mmHg or ≥130/80 mmHg"),
    THREATENING("Hypertensive emergency with end-organ damage");

    private final String description;

    HTASeverity(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
