package tn.esprit.spring.clinicalservice.enums;

/**
 * Treatment modalities for Retinopathy of Prematurity
 */
public enum ROP_Treatment {
    NONE("No treatment"),
    LASER("Laser ablation of avascular retina"),
    INTRAVITREAL_INJECTION("Intravitreal anti-VEGF injection"),
    OTHER("Other surgical intervention");

    private final String description;

    ROP_Treatment(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
