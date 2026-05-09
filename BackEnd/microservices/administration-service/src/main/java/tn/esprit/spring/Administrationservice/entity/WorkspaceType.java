package tn.esprit.spring.Administrationservice.entity;

public enum WorkspaceType {
    ADMIN_OFFICE("Admin Office", "ADM-OFF"),
    HR_OFFICE("HR Office", "HR-OFF"),
    DOCTOR_OFFICE("Doctor Office", "DOC-OFF"),
    OFFICE("Office", "OFF"),
    HOSPITALIZATION_ROOM("Hospitalization Room", "HOSP-ROOM"),
    DIALYSIS_ROOM("Dialysis Room", "DIAL-ROOM"),
    SURGERY_ROOM("Surgery Room", "SURG-ROOM"),
    LABORATORY("Laboratory", "LAB"),
    PHARMACY("Pharmacy", "PHAR"),
    RECEPTION("Reception", "RECP");

    private final String displayName;
    private final String codePrefix;

    WorkspaceType(String displayName, String codePrefix) {
        this.displayName = displayName;
        this.codePrefix = codePrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCodePrefix() {
        return codePrefix;
    }
}
