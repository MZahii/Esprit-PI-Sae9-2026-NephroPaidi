package tn.esprit.spring.clinicalservice.enums;

/**
 * Chronic Kidney Disease (CKD) staging based on eGFR (mL/min/1.73m²)
 * KDIGO classification adapted for pediatric nephrology
 */
public enum CKDStage {
    STAGE_1("Stage 1", "≥90 mL/min", "Normal or high eGFR with evidence of kidney damage"),
    STAGE_2("Stage 2", "60-89 mL/min", "Mild decrease in eGFR with evidence of kidney damage"),
    STAGE_3A("Stage 3a", "45-59 mL/min", "Mild to moderate decrease in eGFR"),
    STAGE_3B("Stage 3b", "30-44 mL/min", "Moderate to severe decrease in eGFR"),
    STAGE_4("Stage 4", "15-29 mL/min", "Severe decrease in eGFR"),
    STAGE_5("Stage 5", "<15 mL/min", "Kidney failure (RRT required)"),
    STAGE_5D("Stage 5D", "<15 mL/min on dialysis", "End-stage renal disease on dialysis");

    private final String label;
    private final String eGFRRange;
    private final String description;

    CKDStage(String label, String eGFRRange, String description) {
        this.label = label;
        this.eGFRRange = eGFRRange;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getEGFRRange() { return eGFRRange; }
    public String getDescription() { return description; }
}
