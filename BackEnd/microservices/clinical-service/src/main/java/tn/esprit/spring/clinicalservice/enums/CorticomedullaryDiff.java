package tn.esprit.spring.clinicalservice.enums;

/**
 * Corticomedullary differentiation on renal ultrasound
 */
public enum CorticomedullaryDiff {
    PRESERVED("Preserved corticomedullary differentiation"),
    REDUCED("Reduced corticomedullary differentiation"),
    ABOLISHED("Abolished corticomedullary differentiation");

    private final String description;

    CorticomedullaryDiff(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
