package tn.esprit.spring.clinicalservice.enums;

/**
 * Discharge destination after hospitalization
 */
public enum DischargeDestination {
    HOME("Home (direct discharge to family)"),
    DECEASED("Patient deceased"),
    OTHER_PEDIATRIC_WARD("Transferred to another pediatric ward"),
    HAD("Admitted to Hospital for Advanced Diagnosis"),
    TRANSFER("Transferred to specialized center"),
    OTHER("Other destination");

    private final String description;

    DischargeDestination(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
