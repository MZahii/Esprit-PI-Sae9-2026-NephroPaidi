package tn.esprit.spring.clinicalservice.enums;

/**
 * Clinical indications for initiation of dialysis therapy
 */
public enum DialysisIndication {
    OLIGURIA("Oliguria: Urine output <0.3 mL/kg/h despite fluid resuscitation"),
    FLUID_OVERLOAD("Fluid overload: >10% weight gain, respiratory distress, hypertension refractory to diuretics"),
    SEVERE_ELECTROLYTE_DISTURBANCE("Severe hyperkalemia (>6.5 mEq/L) or severe acidosis (pH <7.2)"),
    BUN_OVER_80_MG_DL("BUN >80 mg/dL with clinical uremic symptoms");

    private final String description;

    DialysisIndication(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
