package tn.esprit.spring.clinicalservice.enums;

/**
 * Retinopathy of Prematurity (ROP) staging
 */
public enum ROP_Stage {
    NONE("No ROP"),
    STAGE_1("Stage 1: Demarcation line between vascularized and avascular retina"),
    STAGE_2("Stage 2: Ridge with height and width (between line and posterior vascular bed)"),
    STAGE_2_PLUS("Stage 2+: Ridge with extra-retinal fibrovascular proliferation"),
    STAGE_3("Stage 3: Ridge with extra-retinal fibrovascular proliferation"),
    STAGE_3_PLUS("Stage 3+: Stage 3 plus vascular tortuosity and dilation"),
    STAGE_4("Stage 4: Partial retinal detachment"),
    STAGE_5("Stage 5: Total retinal detachment");

    private final String description;

    ROP_Stage(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
