package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecordMovementRequestDTO {
    private String stockType; // EQUIPMENT or DIALYSIS
    private Long itemId;
    private Integer quantityTaken;
    private String requestedBy;
    private String requestedByRole;
    private String purpose;
}