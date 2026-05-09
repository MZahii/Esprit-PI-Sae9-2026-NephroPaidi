package tn.esprit.spring.clinicalservice.enums;

/**
 * Surfactant replacement therapy administration in neonates
 */
public enum Surfactant {
    NOT_DONE("Surfactant not administered"),
    ONE_DOSE("One dose of surfactant"),
    TWO_DOSES("Two doses of surfactant"),
    MORE_THAN_TWO("More than two doses"),
    UNKNOWN("Unknown");

    private final String description;

    Surfactant(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
