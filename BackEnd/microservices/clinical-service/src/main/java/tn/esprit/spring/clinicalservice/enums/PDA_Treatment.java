package tn.esprit.spring.clinicalservice.enums;

/**
 * Treatment approaches for Patent Ductus Arteriosus
 */
public enum PDA_Treatment {
    MEDICAL("Medical: Indomethacin or ibuprofen"),
    SURGICAL("Surgical ligation"),
    ENDOVASCULAR("Endovascular catheter closure"),
    NONE("No treatment (conservative management)");

    private final String description;

    PDA_Treatment(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
