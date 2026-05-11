package tn.esprit.spring.clinicalservice.enums;

/**
 * Type of entry in medical dossier timeline
 */
public enum EntryType {
    CONSULTATION("Clinical consultation"),
    LAB("Laboratory result"),
    HOSPITALIZATION("Hospitalization record"),
    PROCEDURE("Medical procedure"),
    DISCHARGE("Discharge event"),
    PRE_OP_REPORT("Pre-operative report"),
    POST_OP_REPORT("Post-operative report");
    
    private final String description;
    
    EntryType(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
