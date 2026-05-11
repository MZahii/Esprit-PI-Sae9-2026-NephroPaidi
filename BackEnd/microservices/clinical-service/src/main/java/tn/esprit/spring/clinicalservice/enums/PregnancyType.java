package tn.esprit.spring.clinicalservice.enums;

/**
 * Pregnancy type/conception method
 */
public enum PregnancyType {
    SPONTANEOUS("Spontaneous conception"),
    IVF("In Vitro Fertilization"),
    AI("Artificial Insemination"),
    STIMULATION("Ovarian stimulation"),
    OTHER("Other"),
    UNKNOWN("Unknown");

    private final String description;

    PregnancyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
