package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.pharmacyservice.dto.DialysisItemDTO;
import tn.esprit.spring.pharmacyservice.service.DialysisStockService;

import java.util.List;

@RestController
@RequestMapping("/api/dialysis-stock")
@RequiredArgsConstructor
@Tag(name = "Dialysis Stock", description = "Dialysis material inventory management")
public class DialysisStockController {

    private final DialysisStockService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Add a new dialysis material item with initial stock")
    public ResponseEntity<DialysisItemDTO> create(@RequestBody DialysisItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','SURGEON','NURSE')")
    @Operation(summary = "List all dialysis items with current stock")
    public ResponseEntity<List<DialysisItemDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','SURGEON','NURSE')")
    @Operation(summary = "Get dialysis item by ID")
    public ResponseEntity<DialysisItemDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Update dialysis material item details")
    public ResponseEntity<DialysisItemDTO> update(@PathVariable Long id, @RequestBody DialysisItemDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete dialysis item and its stock")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Adjust stock quantity (positive = add, negative = deduct)")
    public ResponseEntity<DialysisItemDTO> adjustStock(
            @PathVariable Long id,
            @RequestParam int delta) {
        return ResponseEntity.ok(service.adjustStock(id, delta));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List dialysis items at or below minimum stock")
    public ResponseEntity<List<DialysisItemDTO>> getLowStock() {
        return ResponseEntity.ok(service.getLowStock());
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List dialysis items with zero stock")
    public ResponseEntity<List<DialysisItemDTO>> getOutOfStock() {
        return ResponseEntity.ok(service.getOutOfStock());
    }
}