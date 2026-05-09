package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicationOrderRequest {
    private String medicationName;
    private Double doseMg;
    private Integer frequencyPerDay;
    private Integer durationDays;
    private String note;
    private Double weightKg;
    private Integer ageYears;
}
