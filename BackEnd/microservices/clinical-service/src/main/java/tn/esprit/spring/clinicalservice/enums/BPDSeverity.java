package tn.esprit.spring.clinicalservice.enums;

/**
 * Severity of Bronchopulmonary Dysplasia (BPD) in neonates
 */
public enum BPDSeverity {
    NONE("No BPD"),
    MILD("Mild BPD: Oxygen requirement resolved by 36 weeks"),
    MODERATE("Moderate BPD: Oxygen requirement at 36 weeks, resolves by discharge"),
    SEVERE("Severe BPD: Oxygen + positive pressure at 36 weeks, prolonged ventilation");

    private final String description;

    BPDSeverity(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
