package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DialysisItemDTO {
    private Long itemId;
    private String name;
    private String category;
    private String unit;
    private Integer minimumStock;
    private String description;
    private Integer currentStock;
    private boolean lowStock;
}