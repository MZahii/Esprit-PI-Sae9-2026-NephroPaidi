package tn.esprit.spring.clinicalservice.enums;

/**
 * Anatomical location of edema/swelling
 */
public enum EdemaLocation {
    PERIORBITAL("Periorbital (around eyes)"),
    LOWER_LIMB("Lower limbs"),
    ASCITES("Ascites (abdominal fluid)"),
    GENERALIZED("Generalized/whole body edema");

    private final String description;

    EdemaLocation(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
