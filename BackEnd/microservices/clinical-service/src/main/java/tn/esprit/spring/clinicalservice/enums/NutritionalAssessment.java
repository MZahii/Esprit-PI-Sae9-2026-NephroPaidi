package tn.esprit.spring.clinicalservice.enums;

/**
 * Nutritional assessment classification
 */
public enum NutritionalAssessment {
    NORMAL("Normal nutritional status"),
    AT_RISK("At risk of malnutrition"),
    MODERATE_MALNUTRITION("Moderate malnutrition"),
    SEVERE_MALNUTRITION("Severe malnutrition");

    private final String description;

    NutritionalAssessment(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
