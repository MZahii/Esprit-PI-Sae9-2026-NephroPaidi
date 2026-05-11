package tn.esprit.spring.clinicalservice.enums;

/**
 * Hearing test result/outcome
 */
public enum HearingResult {
    NORMAL("Normal hearing confirmed"),
    INCONCLUSIVE_UNILATERAL("Inconclusive result on one ear"),
    INCONCLUSIVE_BILATERAL("Inconclusive on both ears"),
    NOT_COMMUNICATED("Result not yet communicated to family");

    private final String description;

    HearingResult(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
