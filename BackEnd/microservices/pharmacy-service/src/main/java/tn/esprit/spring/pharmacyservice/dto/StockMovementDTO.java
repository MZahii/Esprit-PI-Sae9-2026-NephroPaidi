package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovementDTO {
    private Long id;
    private String stockType;
    private Long itemId;
    private String itemName;
    private Integer quantityTaken;
    private String requestedBy;
    private String requestedByRole;
    private String purpose;
    private LocalDateTime takenAt;
}