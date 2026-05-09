package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockDTO {
    private Long stockId;
    private Long batchId;
    private Integer quantityAvailable;
}
