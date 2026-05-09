package tn.esprit.spring.clinicalservice.enums;

/**
 * Nephrotoxic drugs requiring monitoring in renal patients
 */
public enum NephrotoxicDrug {
    NSAIDS("NSAIDs (Non-steroidal anti-inflammatories)"),
    ACE_INHIBITORS("ACE inhibitors"),
    AMINOGLYCOSIDES("Aminoglycoside antibiotics"),
    CEPHALOSPORINS("Cephalosporins"),
    CIPROFLOXACIN("Ciprofloxacin"),
    ACYCLOVIR("Acyclovir"),
    AMPHOTERICIN("Amphotericin B"),
    CONTRAST_AGENTS("Iodinated contrast agents"),
    CISPLATIN("Cisplatin"),
    IFOSFAMIDE("Ifosfamide"),
    CICLOSPORIN("Ciclosporin"),
    CARBAMAZEPINE("Carbamazepine"),
    VALPROATE("Valproate"),
    IV_IMMUNOGLOBULINS("Intravenous immunoglobulins"),
    LITHIUM("Lithium");

    private final String description;

    NephrotoxicDrug(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
