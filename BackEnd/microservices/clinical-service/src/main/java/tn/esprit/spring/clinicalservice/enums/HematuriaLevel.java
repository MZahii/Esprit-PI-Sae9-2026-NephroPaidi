package tn.esprit.spring.clinicalservice.enums;

/**
 * Hematuria severity classification
 */
public enum HematuriaLevel {
    ABSENT("No hematuria"),
    MICROSCOPIC("Microscopic hematuria (requires dipstick or urine culture)"),
    MACROSCOPIC("Macroscopic hematuria (visible to naked eye)");

    private final String description;

    HematuriaLevel(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
