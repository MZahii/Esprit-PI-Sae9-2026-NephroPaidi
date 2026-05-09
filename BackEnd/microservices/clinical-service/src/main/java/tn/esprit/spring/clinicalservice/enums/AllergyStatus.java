package tn.esprit.spring.clinicalservice.enums;

/**
 * Clinical status of the allergy
 */
public enum AllergyStatus {
    ACTIVE("Allergy is currently active/relevant"),
    INACTIVE("No longer applicable"),
    CHRONIC("Chronic/lifelong allergy"),
    INTERMITTENT("Intermittent/seasonal"),
    RECURRENT("Recurrent episodes"),
    RESOLVED("Previously reported, now resolved");

    private final String description;

    AllergyStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
