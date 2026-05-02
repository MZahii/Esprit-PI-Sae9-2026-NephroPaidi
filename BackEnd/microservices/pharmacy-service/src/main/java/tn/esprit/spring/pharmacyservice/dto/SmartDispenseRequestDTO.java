package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartDispenseRequestDTO {
    private Long    medicationId;
    private Integer quantity;
}
