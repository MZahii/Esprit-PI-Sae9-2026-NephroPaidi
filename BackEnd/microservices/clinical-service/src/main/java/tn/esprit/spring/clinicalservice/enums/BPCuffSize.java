package tn.esprit.spring.clinicalservice.enums;

/**
 * Blood pressure cuff size category
 */
public enum BPCuffSize {
    NEONATAL("Neonatal (width 2.6 cm)"),
    INFANT("Infant (width 4.3 cm)"),
    CHILD("Child (width 5.7 cm)"),
    SMALL_ADULT("Small Adult (width 8.4 cm)"),
    ADULT("Adult (width 12.5 cm)"),
    LARGE_ADULT("Large Adult (width 15.6 cm)");
    
    private final String description;
    
    BPCuffSize(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}
