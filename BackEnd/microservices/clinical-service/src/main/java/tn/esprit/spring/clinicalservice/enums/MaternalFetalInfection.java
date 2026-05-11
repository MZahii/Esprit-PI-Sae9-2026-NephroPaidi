package tn.esprit.spring.clinicalservice.enums;

/**
 * Status of maternal-fetal infections (TORCH, GBS, etc.)
 */
public enum MaternalFetalInfection {
    NONE("No maternal-fetal infection"),
    YES_WITHOUT_MENINGITIS("Maternal-fetal infection without meningitis"),
    YES_WITH_MENINGITIS("Maternal-fetal infection with meningitis");

    private final String description;

    MaternalFetalInfection(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
