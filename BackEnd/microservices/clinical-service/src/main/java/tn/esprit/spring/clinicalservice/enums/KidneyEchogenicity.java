package tn.esprit.spring.clinicalservice.enums;

/**
 * Kidney echogenicity findings on ultrasound
 */
public enum KidneyEchogenicity {
    NORMAL("Normal echogenicity"),
    MILDLY_INCREASED("Mildly increased echogenicity"),
    HYPERECHOGENIC("Hyperechogenic (increased echogenicity)");

    private final String description;

    KidneyEchogenicity(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
