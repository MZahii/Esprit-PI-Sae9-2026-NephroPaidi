package tn.esprit.spring.clinicalservice.enums;

/**
 * Types of cardiac pathology in neonates
 */
public enum CardiacPathologyType {
    SEVERE_HEMODYNAMIC_DISORDER("Severe hemodynamic disorder"),
    CONGENITAL_HEART_DISEASE("Congenital Heart Disease"),
    PATENT_DUCTUS_ARTERIOSUS("Patent Ductus Arteriosus (PDA)"),
    VSD("Ventricular Septal Defect"),
    ASD("Atrial Septal Defect"),
    LARGE_PFO("Large Patent Foramen Ovale"),
    OTHER("Other cardiac pathology");

    private final String description;

    CardiacPathologyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
