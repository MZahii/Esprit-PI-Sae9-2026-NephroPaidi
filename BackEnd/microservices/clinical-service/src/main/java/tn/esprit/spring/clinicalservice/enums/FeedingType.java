package tn.esprit.spring.clinicalservice.enums;

/**
 * Type of feeding in neonates/infants
 */
public enum FeedingType {
    BREASTFEEDING("Exclusive breastfeeding"),
    MIXED("Mixed breast and formula"),
    ADAPTED_FORMULA("Adapted infant formula"),
    ENTERAL_TUBE("Enteral tube feeding"),
    HYPOPROTIDIC_PRODUCTS("Hypoprotidic/renal diet products"),
    UNKNOWN("Unknown");

    private final String description;

    FeedingType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
