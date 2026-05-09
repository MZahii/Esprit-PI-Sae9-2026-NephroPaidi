package tn.esprit.spring.clinicalservice.enums;

/**
 * Origin classification for Acute Kidney Injury (AKI)
 */
public enum AKIOrigin {
    PRE_RENAL("Pre-renal: Decreased kidney perfusion (hypotension, dehydration, sepsis)"),
    INTRINSIC_RENAL("Intrinsic renal: Direct kidney damage (glomerulonephritis, acute tubular necrosis, nephrotoxins)"),
    POST_RENAL("Post-renal: Obstruction to urine flow (stones, tumors, ureteral obstruction)");

    private final String description;

    AKIOrigin(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
