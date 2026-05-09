package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplyOrderDTO {
    private Long orderId;
    private Long supplierId;
    private Long medicationId;
    private String itemType;
    private Long itemId;
    private String itemName;
    private LocalDate orderDate;
    private String status;
    private Integer orderedQuantity;
    private Integer deliveredQuantity;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String notes;
}
