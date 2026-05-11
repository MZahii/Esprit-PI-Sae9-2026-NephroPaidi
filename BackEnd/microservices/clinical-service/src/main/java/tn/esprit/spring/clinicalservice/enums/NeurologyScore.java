package tn.esprit.spring.clinicalservice.enums;

/**
 * Neurology screening score for newborn neurological evaluation
 */
public enum NeurologyScore {
    NORMAL_0("Score 0: Normal neurology"),
    DOUBTFUL_1("Score 1: Doubtful (needs repeat)"),
    PATHOLOGICAL_2("Score 2: Pathological (requires further evaluation)");

    private final String description;

    NeurologyScore(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
