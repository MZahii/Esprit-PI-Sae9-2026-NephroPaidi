package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartDispenseRequestDTO {
    private Long    medicationId;
    private Integer quantity;
    private Long    patientId;
    private String  patientName;
    private Long    prescriptionId;
    private String  dispensedBy;
}
