package tn.esprit.spring.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "supply_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyOrder {

    public enum OrderStatus { PENDING, DELIVERED, CANCELLED }

    public enum ItemType { MEDICATION, EQUIPMENT, DIALYSIS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** Nullable for non-medication orders; use itemId + itemType for equipment/dialysis */
    private Long medicationId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ItemType itemType = ItemType.MEDICATION;

    /** ID of the ordered item (medicationId, equipmentItemId, or dialysisItemId) */
    private Long itemId;

    /** Human-readable name kept for display without join */
    private String itemName;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private Integer orderedQuantity;

    /** Actual quantity received — may differ from orderedQuantity */
    private Integer deliveredQuantity;

    private LocalDate expectedDeliveryDate;

    private LocalDate actualDeliveryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public void placeOrder() {
        this.status = OrderStatus.PENDING;
        this.orderDate = LocalDate.now();
    }

    public void cancelOrder() {
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an already delivered order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
