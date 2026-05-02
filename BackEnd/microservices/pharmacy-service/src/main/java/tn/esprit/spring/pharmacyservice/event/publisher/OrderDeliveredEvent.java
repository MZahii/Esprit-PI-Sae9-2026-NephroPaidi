package tn.esprit.spring.pharmacyservice.event.publisher;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Fired when a SupplyOrder transitions to DELIVERED.
 * The StockEventConsumer listens to this to update inventory.
 */
@Getter
public class OrderDeliveredEvent extends ApplicationEvent {

    private final Long orderId;
    private final Long medicationId;
    private final Integer deliveredQuantity;

    public OrderDeliveredEvent(Long orderId, Long medicationId, Integer deliveredQuantity) {
        super(orderId);
        this.orderId = orderId;
        this.medicationId = medicationId;
        this.deliveredQuantity = deliveredQuantity;
    }
}
