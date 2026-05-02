package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tn.esprit.spring.pharmacyservice.dto.DispenseRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.DispensationLogDTO;
import tn.esprit.spring.pharmacyservice.dto.SmartDispenseRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.SmartDispenseResponseDTO;
import tn.esprit.spring.pharmacyservice.dto.StockDTO;
import tn.esprit.spring.pharmacyservice.dto.TransferStockRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.TransferStockResponseDTO;
import tn.esprit.spring.pharmacyservice.repository.MedicationRepository;
import tn.esprit.spring.pharmacyservice.entity.Batch;
import tn.esprit.spring.pharmacyservice.entity.DispensationLog;
import tn.esprit.spring.pharmacyservice.entity.Stock;
import tn.esprit.spring.pharmacyservice.repository.BatchRepository;
import tn.esprit.spring.pharmacyservice.repository.DispensationLogRepository;
import tn.esprit.spring.pharmacyservice.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final BatchRepository batchRepository;
    private final DispensationLogRepository dispensationLogRepository;
    private final MedicationRepository medicationRepository;

    // ─── Initialization ───────────────────────────────────────────────────────

    /**
     * Called when a Batch is first created to initialise its stock entry.
     */
    public StockDTO initializeStock(Long batchId, int initialQty) {
        if (stockRepository.findByBatchId(batchId).isPresent()) {
            throw new IllegalStateException("Stock already exists for batch: " + batchId);
        }
        Stock stock = Stock.builder()
                .batchId(batchId)
                .quantityAvailable(initialQty)
                .build();
        return toDTO(stockRepository.save(stock));
    }

    // ─── Reads ────────────────────────────────────────────────────────────────

    public StockDTO getStockByBatch(Long batchId) {
        return toDTO(findByBatchId(batchId));
    }

    public List<StockDTO> getAllStock() {
        return getAllStock(null);
    }

    /**
     * @param sort "asc" → quantity low→high, "desc" → high→low, null → default
     */
    public List<StockDTO> getAllStock(String sort) {
        List<StockDTO> list = stockRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
        if ("asc".equalsIgnoreCase(sort))
            list.sort(Comparator.comparingInt(StockDTO::getQuantityAvailable));
        else if ("desc".equalsIgnoreCase(sort))
            list.sort(Comparator.comparingInt(StockDTO::getQuantityAvailable).reversed());
        return list;
    }

    public List<StockDTO> getLowStock(int threshold) {
        return stockRepository.findLowStock(threshold).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<StockDTO> getOutOfStock() {
        return stockRepository.findOutOfStock().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    /**
     * Increase stock when a supply order is delivered.
     */
    public StockDTO receiveDelivery(Long batchId, int qty) {
        Stock stock = findByBatchId(batchId);
        stock.updateStock(qty);
        log.info("Stock updated for batch {}: +{} units. New total: {}", batchId, qty, stock.getQuantityAvailable());
        return toDTO(stockRepository.save(stock));
    }

    /**
     * Dispense medication (decrease stock).
     */
    public StockDTO dispense(DispenseRequestDTO request) {
        Long batchId = request.getBatchId();
        int qty = request.getQuantity();

        // Guard: expired batch check
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NoSuchElementException("Batch not found: " + batchId));
        if (batch.isExpired()) {
            throw new IllegalStateException("Cannot dispense from expired batch: " + batchId);
        }

        Stock stock = findByBatchId(batchId);
        if (!stock.checkAvailability(qty)) {
            throw new IllegalStateException(
                    "Insufficient stock for batch " + batchId + ". Requested: " + qty
                            + ", Available: " + stock.getQuantityAvailable());
        }

        stock.updateStock(-qty);
        log.info("Dispensed {} units from batch {} for patient {}",
                qty, batchId); //, request.getPatientId()  put it in
        StockDTO result = toDTO(stockRepository.save(stock));

        dispensationLogRepository.save(DispensationLog.builder()
                .batchId(batchId)
                .quantity(qty)
                .dispensedAt(LocalDateTime.now())
                .build());

        return result;
    }

    /**
     * Manual adjustment (positive or negative).
     */
    public StockDTO adjustStock(Long batchId, int delta, String reason) {
        Stock stock = findByBatchId(batchId);
        stock.updateStock(delta);
        log.info("Manual stock adjustment for batch {}: {} (reason: {})", batchId, delta, reason);
        return toDTO(stockRepository.save(stock));
    }

    // ─── Stock Transfer ───────────────────────────────────────────────────────

    /**
     * Transfers stock from one batch to another.
     * Validates source availability and target batch is not expired.
     */
    public TransferStockResponseDTO transferStock(TransferStockRequestDTO request) {
        Long sourceId = request.getSourceBatchId();
        Long targetId = request.getTargetBatchId();
        int qty = request.getQuantity();

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Source and target batches must be different.");
        }

        Batch targetBatch = batchRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("Target batch not found: " + targetId));
        if (targetBatch.isExpired()) {
            throw new IllegalStateException("Cannot transfer stock to an expired batch: " + targetId);
        }

        Stock source = findByBatchId(sourceId);
        if (!source.checkAvailability(qty)) {
            throw new IllegalStateException("Insufficient stock in source batch " + sourceId
                    + ". Requested: " + qty + ", Available: " + source.getQuantityAvailable());
        }

        Stock target = findByBatchId(targetId);

        source.updateStock(-qty);
        target.updateStock(qty);
        stockRepository.save(source);
        stockRepository.save(target);

        String reason = request.getReason() != null ? request.getReason() : "stock transfer";
        log.info("Transferred {} units from batch {} to batch {} (reason: {})", qty, sourceId, targetId, reason);

        return TransferStockResponseDTO.builder()
                .sourceBatchId(sourceId)
                .targetBatchId(targetId)
                .quantityTransferred(qty)
                .sourceQuantityAvailable(source.getQuantityAvailable())
                .targetQuantityAvailable(target.getQuantityAvailable())
                .reason(reason)
                .build();
    }

    // ─── Smart FEFO Dispense ──────────────────────────────────────────────────

    /**
     * FEFO (First Expired First Out): automatically picks non-expired batches
     * ordered by expiration date and dispenses across them until quantity is fulfilled.
     */
    public SmartDispenseResponseDTO smartDispense(SmartDispenseRequestDTO request) {
        Long medicationId = request.getMedicationId();
        int requested = request.getQuantity();

        String medicationName = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new NoSuchElementException("Medication not found: " + medicationId))
                .getName();

        List<Batch> batches = batchRepository.findNonExpiredByMedicationOrderedByExpiry(medicationId, LocalDate.now());
        if (batches.isEmpty()) {
            throw new IllegalStateException("No valid (non-expired) batches available for medication: " + medicationId);
        }

        List<SmartDispenseResponseDTO.BatchDispenseLineDTO> lines = new ArrayList<>();
        int remaining = requested;

        for (Batch batch : batches) {
            if (remaining <= 0) break;

            Stock stock = stockRepository.findByBatchId(batch.getBatchId()).orElse(null);
            if (stock == null || stock.getQuantityAvailable() <= 0) continue;

            int toDispense = Math.min(remaining, stock.getQuantityAvailable());
            stock.updateStock(-toDispense);
            stockRepository.save(stock);

            dispensationLogRepository.save(DispensationLog.builder()
                    .batchId(batch.getBatchId())
                    .quantity(toDispense)
                    .dispensedAt(LocalDateTime.now())
                    .build());

            lines.add(SmartDispenseResponseDTO.BatchDispenseLineDTO.builder()
                    .batchId(batch.getBatchId())
                    .batchNumber(batch.getBatchNumber())
                    .quantityDispensed(toDispense)
                    .expirationDate(batch.getExpirationDate().toString())
                    .build());

            remaining -= toDispense;
            log.info("Smart dispense: {} units from batch {} (exp: {})", toDispense, batch.getBatchId(), batch.getExpirationDate());
        }

        if (remaining > 0) {
            throw new IllegalStateException("Insufficient total stock. Still missing " + remaining + " unit(s) for medication: " + medicationId);
        }

        return SmartDispenseResponseDTO.builder()
                .medicationId(medicationId)
                .medicationName(medicationName)
                .requested(requested)
                .totalDispensed(requested)
                .lines(lines)
                .build();
    }

    // ─── Dispensation History ─────────────────────────────────────────────────

    public List<DispensationLogDTO> getDispensationHistory(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(23, 59, 59);
        return dispensationLogRepository
                .findByDispensedAtBetweenOrderByDispensedAtDesc(start, end)
                .stream().map(this::toLogDTO).collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Stock findByBatchId(Long batchId) {
        return stockRepository.findByBatchId(batchId)
                .orElseThrow(() -> new NoSuchElementException("No stock entry for batch: " + batchId));
    }

    private StockDTO toDTO(Stock s) {
        return StockDTO.builder()
                .stockId(s.getStockId())
                .batchId(s.getBatchId())
                .quantityAvailable(s.getQuantityAvailable())
                .build();
    }

    private DispensationLogDTO toLogDTO(DispensationLog l) {
        return DispensationLogDTO.builder()
                .id(l.getId())
                .batchId(l.getBatchId())
                .quantity(l.getQuantity())
                .dispensedAt(l.getDispensedAt())
                .build();
    }
}
