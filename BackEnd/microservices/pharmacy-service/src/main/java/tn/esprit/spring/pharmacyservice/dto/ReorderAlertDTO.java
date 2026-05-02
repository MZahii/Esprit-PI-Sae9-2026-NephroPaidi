package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReorderAlertDTO {
    private Long   medicationId;
    private String name;
    private String form;
    private Integer minimumStock;
    private Integer currentTotalStock;
    private Integer deficit;
}
