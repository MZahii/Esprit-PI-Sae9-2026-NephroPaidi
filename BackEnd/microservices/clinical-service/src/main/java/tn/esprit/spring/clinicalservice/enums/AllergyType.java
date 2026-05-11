package tn.esprit.spring.clinicalservice.enums;

/**
 * Type of allergy
 */
public enum AllergyType {
    DRUG("Drug allergy"),
    FOOD("Food allergy"),
    ENVIRONMENTAL("Environmental allergy"),
    LATEX("Latex allergy"),
    CONTRAST_AGENT("Contrast agent allergy"),
    OTHER("Other allergy type");

    private final String description;

    AllergyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
