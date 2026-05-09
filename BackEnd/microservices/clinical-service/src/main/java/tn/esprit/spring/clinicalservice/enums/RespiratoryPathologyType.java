package tn.esprit.spring.clinicalservice.enums;

/**
 * Types of respiratory pathology in neonates
 */
public enum RespiratoryPathologyType {
    HMD("Hyaline Membrane Disease (RDS)"),
    TTN("Transient Tachypnea of the Newborn"),
    PPHN("Persistent Pulmonary Hypertension of the Newborn"),
    MECONIUM_ASPIRATION("Meconium Aspiration Syndrome"),
    OTHER("Other respiratory pathology");

    private final String description;

    RespiratoryPathologyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
