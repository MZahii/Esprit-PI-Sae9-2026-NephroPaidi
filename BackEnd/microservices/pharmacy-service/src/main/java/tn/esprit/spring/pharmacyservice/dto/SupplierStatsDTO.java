package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierStatsDTO {
    private Long supplierId;
    private String supplierName;
    private long totalOrders;
    private long deliveredOrders;
    private long pendingOrders;
    private long cancelledOrders;
    private long overdueOrders;
    private double deliveryRate; // % of non-cancelled orders that were delivered
}
