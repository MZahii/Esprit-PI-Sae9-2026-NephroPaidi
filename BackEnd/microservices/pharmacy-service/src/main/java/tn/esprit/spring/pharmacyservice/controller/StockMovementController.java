package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.pharmacyservice.dto.RecordMovementRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.StockMovementDTO;
import tn.esprit.spring.pharmacyservice.service.StockMovementService;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Outgoing stock tracking for equipment and dialysis materials")
public class StockMovementController {

    private final StockMovementService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Record an outgoing stock movement (automatically deducts from stock)")
    public ResponseEntity<StockMovementDTO> record(@RequestBody RecordMovementRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordMovement(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Get all stock movements (most recent first)")
    public ResponseEntity<List<StockMovementDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/type/{stockType}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Get movements by stock type (EQUIPMENT or DIALYSIS)")
    public ResponseEntity<List<StockMovementDTO>> getByType(@PathVariable String stockType) {
        return ResponseEntity.ok(service.getByType(stockType));
    }

    @GetMapping("/item/{itemId}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Get movements for a specific item")
    public ResponseEntity<List<StockMovementDTO>> getByItem(
            @PathVariable Long itemId,
            @RequestParam String stockType) {
        return ResponseEntity.ok(service.getByItemId(itemId, stockType));
    }
}