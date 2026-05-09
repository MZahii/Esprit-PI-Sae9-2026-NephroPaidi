package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.pharmacyservice.dto.EquipmentItemDTO;
import tn.esprit.spring.pharmacyservice.service.EquipmentService;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment Stock", description = "Medical equipment inventory management")
public class EquipmentController {

    private final EquipmentService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Add a new equipment item with initial stock")
    public ResponseEntity<EquipmentItemDTO> create(@RequestBody EquipmentItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE','SURGEON')")
    @Operation(summary = "List all equipment items with current stock")
    public ResponseEntity<List<EquipmentItemDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','NURSE','SURGEON')")
    @Operation(summary = "Get equipment item by ID")
    public ResponseEntity<EquipmentItemDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Update equipment item details")
    public ResponseEntity<EquipmentItemDTO> update(@PathVariable Long id, @RequestBody EquipmentItemDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete equipment item and its stock")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Adjust stock quantity (positive = add, negative = deduct)")
    public ResponseEntity<EquipmentItemDTO> adjustStock(
            @PathVariable Long id,
            @RequestParam int delta) {
        return ResponseEntity.ok(service.adjustStock(id, delta));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List equipment items at or below minimum stock")
    public ResponseEntity<List<EquipmentItemDTO>> getLowStock() {
        return ResponseEntity.ok(service.getLowStock());
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List equipment items with zero stock")
    public ResponseEntity<List<EquipmentItemDTO>> getOutOfStock() {
        return ResponseEntity.ok(service.getOutOfStock());
    }
}