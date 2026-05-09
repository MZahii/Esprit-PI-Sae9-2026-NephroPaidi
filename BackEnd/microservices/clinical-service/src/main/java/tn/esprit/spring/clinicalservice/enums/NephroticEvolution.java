package tn.esprit.spring.clinicalservice.enums;

/**
 * Evolution pattern of Nephrotic Syndrome
 */
public enum NephroticEvolution {
    DEFINITIVE_REMISSION("Achieves sustained remission without immunosuppression"),
    SPACED_RELAPSES("Multiple relapses but with remission periods between"),
    STEROID_DEPENDENT("Relapses during or shortly after steroid tapering"),
    FREQUENT_RELAPSER("4+ relapses in 12 months, difficult to manage");

    private final String description;

    NephroticEvolution(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
