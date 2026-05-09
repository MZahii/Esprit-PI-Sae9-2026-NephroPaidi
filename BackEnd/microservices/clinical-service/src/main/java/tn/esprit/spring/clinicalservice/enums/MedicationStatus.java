package tn.esprit.spring.clinicalservice.enums;

/**
 * Medication status at discharge or medication management changes
 */
public enum MedicationStatus {
    CONTINUED("Medication continued from hospitalization"),
    MODIFIED("Medication dose/frequency modified"),
    NEW("New medication started at discharge"),
    STOPPED("Medication stopped at discharge");

    private final String description;

    MedicationStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
