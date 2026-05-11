package tn.esprit.spring.clinicalservice.enums;

/**
 * Hearing screening technique/method used
 */
public enum HearingTechnique {
    AUTOMATED_ABR("Automated Auditory Brainstem Response"),
    OAE("Otoacoustic Emissions"),
    THRESHOLD_ABR("Threshold Auditory Brainstem Response");

    private final String description;

    HearingTechnique(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
