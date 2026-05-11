package tn.esprit.spring.clinicalservice.enums;

/**
 * Severity of allergic reaction
 */
public enum AllergySeverity {
    LOW("Low: Mild local reaction (itching, rash)"),
    MODERATE("Moderate: Systemic symptoms (urticaria, mild swelling)"),
    HIGH("High: Severe reaction (airway compromise, anaphylaxis risk)"),
    ANAPHYLAXIS("Anaphylaxis: Severe life-threatening reaction");

    private final String description;

    AllergySeverity(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
