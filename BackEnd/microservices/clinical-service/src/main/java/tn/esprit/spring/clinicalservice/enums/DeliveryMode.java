package tn.esprit.spring.clinicalservice.enums;

/**
 * Mode of delivery/delivery method
 */
public enum DeliveryMode {
    NORMAL_VAGINAL("Normal vaginal delivery"),
    INSTRUMENTAL_VAGINAL("Instrumental vaginal delivery (forceps/vacuum)"),
    CSECTION_PRE_LABOR("Cesarean section pre-labor"),
    CSECTION_IN_LABOR("Cesarean section in labor");

    private final String description;

    DeliveryMode(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
