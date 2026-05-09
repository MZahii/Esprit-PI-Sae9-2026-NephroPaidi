package tn.esprit.spring.clinicalservice.enums;

/**
 * Response to corticosteroid therapy in nephrotic syndrome
 */
public enum CorticosteroidResponse {
    SENSITIVE("Responds to steroids, achieves remission within 2-4 weeks"),
    RESISTANT("No response to steroids, <4g/day proteinuria reduction after 4 weeks"),
    DEPENDENT("Relapses during or within 2 weeks of steroid tapering"),
    FREQUENT_RELAPSER("4 or more relapses in 12 months");

    private final String description;

    CorticosteroidResponse(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
