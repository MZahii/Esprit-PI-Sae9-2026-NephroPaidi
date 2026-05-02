package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.DispensationLogDTO;
import tn.esprit.spring.pharmacyservice.dto.DispenseRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.SmartDispenseRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.SmartDispenseResponseDTO;
import tn.esprit.spring.pharmacyservice.dto.StockDTO;
import tn.esprit.spring.pharmacyservice.dto.TransferStockRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.TransferStockResponseDTO;
import tn.esprit.spring.pharmacyservice.service.StockService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Inventory & dispensation management")
public class StockController {

    private final StockService stockService;

    @PostMapping("/batches/{batchId}/initialize")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Initialize stock for a new batch")
    public ResponseEntity<StockDTO> initialize(
            @PathVariable Long batchId,
            @RequestParam int quantity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.initializeStock(batchId, quantity));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE','GUARDIAN')")
    @Operation(summary = "List all stock entries with optional sorting",
               description = "Supports ?sort=asc (low→high quantity) or ?sort=desc (high→low)")
    public ResponseEntity<List<StockDTO>> getAll(
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(stockService.getAllStock(sort));
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE')")
    @Operation(summary = "Get stock for a specific batch")
    public ResponseEntity<StockDTO> getByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(stockService.getStockByBatch(batchId));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE')")
    @Operation(summary = "List low-stock entries below threshold")
    public ResponseEntity<List<StockDTO>> getLow(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(stockService.getLowStock(threshold));
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE')")
    @Operation(summary = "List completely out-of-stock batches")
    public ResponseEntity<List<StockDTO>> getOutOfStock() {
        return ResponseEntity.ok(stockService.getOutOfStock());
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST','NURSE','ADMIN')")
    @Operation(summary = "Dispense medication from a batch")
    public ResponseEntity<StockDTO> dispense(@RequestBody DispenseRequestDTO request) {
        return ResponseEntity.ok(stockService.dispense(request));
    }

    @GetMapping("/dispensations")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE')")
    @Operation(summary = "Dispensation history for a given date (defaults to today)")
    public ResponseEntity<List<DispensationLogDTO>> getDispensations(
            @RequestParam(required = false) String date) {
        LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(stockService.getDispensationHistory(d));
    }

    @PatchMapping("/batches/{batchId}/adjust")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Manual stock adjustment (positive or negative delta)")
    public ResponseEntity<StockDTO> adjust(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> body) {
        int delta = (int) body.get("delta");
        String reason = (String) body.getOrDefault("reason", "manual adjustment");
        return ResponseEntity.ok(stockService.adjustStock(batchId, delta, reason));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Transfer stock from one batch to another")
    public ResponseEntity<TransferStockResponseDTO> transfer(@RequestBody TransferStockRequestDTO request) {
        return ResponseEntity.ok(stockService.transferStock(request));
    }

    @PostMapping("/smart-dispense")
    @PreAuthorize("hasAnyRole('PHARMACIST','NURSE','ADMIN')")
    @Operation(summary = "FEFO smart dispense: automatically picks batches expiring soonest first")
    public ResponseEntity<SmartDispenseResponseDTO> smartDispense(@RequestBody SmartDispenseRequestDTO request) {
        return ResponseEntity.ok(stockService.smartDispense(request));
    }
}