package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicationDTO {
    private Long   medicationId;
    private String name;
    private String genericName;
    private String form;
    private String strength;
    private String unit;
    private String therapeuticClass;
    private String standardDosage;
    private boolean renalDoseAdjustment;
    private String storageConditions;
    private boolean controlledSubstance;
    private Integer minimumStock;
}
