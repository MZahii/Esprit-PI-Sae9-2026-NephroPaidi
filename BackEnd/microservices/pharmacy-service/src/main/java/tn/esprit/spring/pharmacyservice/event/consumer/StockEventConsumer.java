package tn.esprit.spring.pharmacyservice.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.esprit.spring.pharmacyservice.event.publisher.OrderDeliveredEvent;
import tn.esprit.spring.pharmacyservice.repository.StockRepository;
import tn.esprit.spring.pharmacyservice.repository.BatchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to OrderDeliveredEvent:
 * Finds the most recent non-expired batch for the medication
 * and credits the delivered quantity to its stock entry.
 *
 * If no matching stock entry exists yet, logs a warning —
 * stock must be initialised via POST /api/stock/batches/{id}/initialize first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final StockRepository stockRepository;
    private final BatchRepository batchRepository;

    @EventListener
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("OrderDeliveredEvent received — orderId={}, medicationId={}, qty={}",
                event.getOrderId(), event.getMedicationId(), event.getDeliveredQuantity());

        // Find batches for this medication ordered by expiration descending (freshest first)
        batchRepository.findByMedicationMedicationId(event.getMedicationId()).stream()
                .filter(b -> !b.isExpired())
                .findFirst()
                .ifPresentOrElse(batch -> {
                    stockRepository.findByBatchId(batch.getBatchId()).ifPresentOrElse(
                            stock -> {
                                stock.updateStock(event.getDeliveredQuantity());
                                stockRepository.save(stock);
                                log.info("Stock updated for batchId={} +{} units",
                                        batch.getBatchId(), event.getDeliveredQuantity());
                            },
                            () -> log.warn("No stock entry for batchId={}. Initialize it first.",
                                    batch.getBatchId())
                    );
                }, () -> log.warn("No valid non-expired batch found for medicationId={}",
                        event.getMedicationId()));
    }
}
