package tn.esprit.spring.clinicalservice.enums;

/**
 * Histological findings on renal biopsy
 */
public enum BiopsyHistology {
    MINIMAL_CHANGE("Minimal Change Disease"),
    FSGS("Focal Segmental Glomerulosclerosis"),
    MEMBRANOPROLIFERATIVE("Membranoproliferative Glomerulonephritis"),
    MEMBRANOUS("Membranous Nephropathy"),
    IGA_NEPHROPATHY("IgA Nephropathy"),
    MESANGIAL_SCLEROSIS("Mesangial Sclerosis"),
    CORTICAL_NECROSIS("Cortical Necrosis"),
    MICROANGIOPATHY("Thrombotic Microangiopathy"),
    OTHER("Other histological finding");

    private final String description;

    BiopsyHistology(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
