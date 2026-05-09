package tn.esprit.spring.clinicalservice.enums;

/**
 * Infectious agents causing neonatal sepsis
 */
public enum InfectiousAgent {
    E_COLI("E. coli (K1)"),
    STREP_B("Group B Streptococcus"),
    ENTEROBACTER("Enterobacter species"),
    CANDIDA("Candida species"),
    CMV("Cytomegalovirus"),
    OTHER("Other pathogens");

    private final String description;

    InfectiousAgent(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
