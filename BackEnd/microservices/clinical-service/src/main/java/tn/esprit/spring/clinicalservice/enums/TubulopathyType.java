package tn.esprit.spring.clinicalservice.enums;

/**
 * Types of primary tubulopathies (inherited or acquired tubular dysfunction)
 */
public enum TubulopathyType {
    BARTTER("Bartter Syndrome (salt-wasting, hypokalemia, alkalosis)"),
    GITELMAN("Gitelman Syndrome (milder variant of Bartter)"),
    LIDDLE("Liddle Syndrome (pseudohyperaldosteronism)"),
    DENT_DISEASE("Dent Disease (X-linked recessive low-molecular-weight proteinuria)"),
    FANCONI_SYNDROME("Fanconi Syndrome (proximal tubule dysfunction)"),
    HYPOPHOSPHATEMIC_RICKETS("Hypophosphatemic Rickets (PHEX mutation)"),
    DISTAL_RTA("Distal (Type 1) Renal Tubular Acidosis"),
    PROXIMAL_RTA("Proximal (Type 2) Renal Tubular Acidosis"),
    PSEUDO_BARTTER("Pseudo-Bartter Syndrome (from chronic diuretic use or vomiting)");

    private final String description;

    TubulopathyType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
