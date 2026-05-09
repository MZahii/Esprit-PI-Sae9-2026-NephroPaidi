package tn.esprit.spring.clinicalservice.enums;

/**
 * Congenital Anomalies of the Kidney and Urinary Tract (CAKUT) subtypes
 */
public enum CAKUTSubtype {
    RENAL_AGENESIS_UNILATERAL("Unilateral Renal Agenesis"),
    RENAL_AGENESIS_BILATERAL("Bilateral Renal Agenesis"),
    RENAL_HYPOPLASIA("Renal Hypoplasia"),
    RENAL_DYSPLASIA("Renal Dysplasia"),
    MULTICYSTIC_DYSPLASTIC_KIDNEY("Multicystic Dysplastic Kidney"),
    HORSESHOE_KIDNEY("Horseshoe Kidney"),
    ADPKD("Autosomal Dominant Polycystic Kidney Disease"),
    ARPKD("Autosomal Recessive Polycystic Kidney Disease"),
    VESICOURETERAL_REFLUX("Vesicoureteral Reflux"),
    UPJ_OBSTRUCTION("Ureteropelvic Junction Obstruction"),
    MEGAURETER("Megaureter"),
    POSTERIOR_URETHRAL_VALVES("Posterior Urethral Valves"),
    ISOLATED_CYST("Isolated Renal Cyst");

    private final String description;

    CAKUTSubtype(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
