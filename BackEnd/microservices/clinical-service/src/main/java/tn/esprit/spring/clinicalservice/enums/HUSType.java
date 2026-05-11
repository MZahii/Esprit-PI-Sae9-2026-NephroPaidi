package tn.esprit.spring.clinicalservice.enums;

/**
 * Hemolytic Uremic Syndrome (HUS) classification
 */
public enum HUSType {
    TYPICAL_STEC("STEC-HUS: typical HUS caused by Shiga toxin-producing E. coli"),
    ATYPICAL("Atypical HUS: non-STEC related (genetic, secondary, or unknown cause)");

    private final String description;

    HUSType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
