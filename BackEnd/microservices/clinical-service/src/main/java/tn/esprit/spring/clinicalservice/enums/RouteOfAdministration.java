package tn.esprit.spring.clinicalservice.enums;

/**
 * Route of medication administration
 */
public enum RouteOfAdministration {
    ORAL("By mouth"),
    IV("Intravenous"),
    SC("Subcutaneous"),
    IM("Intramuscular"),
    INHALED("Inhaled"),
    TOPICAL("Applied to skin"),
    RECTAL("Rectal suppository"),
    NASAL("Nasal spray/drops");

    private final String description;

    RouteOfAdministration(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
