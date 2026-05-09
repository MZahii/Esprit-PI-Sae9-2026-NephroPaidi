package tn.esprit.spring.clinicalservice.enums;

/**
 * Proteinuria severity classification
 */
public enum ProteinuriaCategory {
    ABSENT("No proteinuria detected", 0.0),
    TRACE("Trace proteinuria", 0.15),
    MILD("Mild proteinuria (non-nephrotic)", 1.0),
    NEPHROTIC_RANGE("Nephrotic range proteinuria (>3.5g/24h)", 3.5);

    private final String description;
    private final double thresholdGramsPer24h;

    ProteinuriaCategory(String description, double thresholdGramsPer24h) {
        this.description = description;
        this.thresholdGramsPer24h = thresholdGramsPer24h;
    }

    public String getDescription() { return description; }
    public double getThresholdGramsPer24h() { return thresholdGramsPer24h; }
}
