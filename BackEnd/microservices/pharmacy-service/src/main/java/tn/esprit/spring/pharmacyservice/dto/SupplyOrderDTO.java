package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplyOrderDTO {
    private Long orderId;
    private Long supplierId;
    private Long medicationId;
    private LocalDate orderDate;
    private String status;
    private Integer orderedQuantity;
    private LocalDate expectedDeliveryDate;
    private String notes;
}
