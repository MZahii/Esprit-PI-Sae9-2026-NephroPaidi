package tn.esprit.spring.clinicalservice.enums;

/**
 * Status of CRH (Compte Rendu d'Hospitalisation) discharge document
 */
public enum CRHDocumentStatus {
    COMPLETE("Complete discharge document"),
    PARTIAL_PENDING_8_DAYS("Partial document pending completion within 8 days");

    private final String description;

    CRHDocumentStatus(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
