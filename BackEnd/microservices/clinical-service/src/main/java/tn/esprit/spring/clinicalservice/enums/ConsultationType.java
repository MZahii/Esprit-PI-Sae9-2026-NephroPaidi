package tn.esprit.spring.clinicalservice.enums;

/**
 * Consultation type (encounter classification)
 */
public enum ConsultationType {
    CRC("Consultation de Routine Clinique (Routine Clinical Consultation)"),
    CRH("Consultation Raison Hospitalisation (Consultation for Hospitalization)"),
    CRO("Consultation Raison Opération (Consultation for Operation)"),
    CREO("Consultation Raison Examen Œuvre (Consultation for Testing)"),
    CRMO("Consultation Raison Multiples Objectives (Consultation for Multiple Objectives)"),
    CRIO("Consultation Raison Infirmière Observation (Consultation for Nursing Observation)");

    private final String description;

    ConsultationType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
