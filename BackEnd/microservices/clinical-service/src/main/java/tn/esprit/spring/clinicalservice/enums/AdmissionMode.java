package tn.esprit.spring.clinicalservice.enums;

/**
 * Mode of patient admission
 */
public enum AdmissionMode {
    SCHEDULED("Scheduled/elective admission"),
    EMERGENCY("Emergency admission"),
    TRANSFER("Transfer from another facility"),
    REFERRAL("Referral from ambulatory setting");

    private final String description;

    AdmissionMode(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
