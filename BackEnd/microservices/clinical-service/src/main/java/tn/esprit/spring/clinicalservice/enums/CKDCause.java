package tn.esprit.spring.clinicalservice.enums;

/**
 * Underlying causes of Chronic Kidney Disease
 */
public enum CKDCause {
    CAKUT("Congenital Anomalies of the Kidney and Urinary Tract"),
    GLOMERULONEPHRITIS("Glomerulonephritis"),
    HEREDITARY_NEPHROPATHY("Hereditary Nephropathy"),
    IGA_NEPHROPATHY("IgA Nephropathy"),
    ALPORT_SYNDROME("Alport Syndrome"),
    FSGS("Focal Segmental Glomerulosclerosis"),
    LUPUS_NEPHRITIS("Lupus Nephritis"),
    HUS("Hemolytic Uremic Syndrome"),
    REFLUX_NEPHROPATHY("Reflux Nephropathy"),
    POLYCYSTIC_KIDNEY("Polycystic Kidney Disease"),
    TUBULOPATHY("Tubulopathy"),
    ANCA_VASCULITIS("ANCA-associated Vasculitis"),
    SYSTEMIC_DISEASE("Systemic Disease"),
    OTHER("Other"),
    UNKNOWN("Unknown");

    private final String description;

    CKDCause(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
