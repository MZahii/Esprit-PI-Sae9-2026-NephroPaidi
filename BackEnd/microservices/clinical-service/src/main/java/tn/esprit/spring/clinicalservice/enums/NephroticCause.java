package tn.esprit.spring.clinicalservice.enums;

/**
 * Underlying causes of Nephrotic Syndrome
 */
public enum NephroticCause {
    MINIMAL_CHANGE("Minimal Change Nephrotic Syndrome"),
    MEMBRANOPROLIFERATIVE("Membranoproliferative Glomerulonephritis"),
    FSGS("Focal Segmental Glomerulosclerosis"),
    MEMBRANOUS("Membranous Nephropathy"),
    HSP("Henoch-Schönlein Purpura"),
    LUPUS("Lupus Nephritis"),
    HBV_HCV("Hepatitis B/C Associated"),
    HIV("HIV-associated Glomerulonephritis"),
    OTHER("Other");

    private final String description;

    NephroticCause(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
