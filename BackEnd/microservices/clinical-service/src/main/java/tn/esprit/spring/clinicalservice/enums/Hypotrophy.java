package tn.esprit.spring.clinicalservice.enums;

/**
 * Intrauterine growth restriction (small for gestational age) severity
 */
public enum Hypotrophy {
    NONE("Normal birth weight (>10th percentile)"),
    BELOW_10TH_PERCENTILE("Below 10th percentile (mild intrauterine growth restriction)"),
    BELOW_3RD_PERCENTILE("Below 3rd percentile (severe intrauterine growth restriction)");

    private final String description;

    Hypotrophy(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
