package tn.esprit.spring.clinicalservice.enums;

/**
 * Vision screening score for newborn ocular evaluation
 */
public enum VisionScore {
    NORMAL_0("Score 0: Normal vision"),
    DOUBTFUL_1("Score 1: Doubtful (needs repeat)"),
    PATHOLOGICAL_2("Score 2: Pathological (requires further evaluation)");

    private final String description;

    VisionScore(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
