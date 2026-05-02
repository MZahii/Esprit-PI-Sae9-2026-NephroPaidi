package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDTO {
    private Long medicationId;
    private String name;
    private String form;
    private String pediatricDosage;
    private Integer minimumStock;
}
