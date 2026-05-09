package tn.esprit.spring.clinicalservice.enums;

/**
 * Hearing screening score for newborn auditory evaluation
 */
public enum HearingScore {
    NORMAL_0("Score 0: Normal hearing"),
    DOUBTFUL_1("Score 1: Doubtful (needs repeat)"),
    PATHOLOGICAL_2("Score 2: Pathological (requires further evaluation)");

    private final String description;

    HearingScore(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
