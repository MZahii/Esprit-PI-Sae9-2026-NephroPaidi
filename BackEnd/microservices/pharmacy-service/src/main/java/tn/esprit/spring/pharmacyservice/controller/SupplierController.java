package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.SupplierDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplierStatsDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplyOrderDTO;
import tn.esprit.spring.pharmacyservice.service.SupplierService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier & supply order management")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Register a supplier")
    public ResponseEntity<SupplierDTO> create(@RequestBody SupplierDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createSupplier(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List suppliers with optional filters",
               description = "Supports ?name=, ?active=true|false")
    public ResponseEntity<List<SupplierDTO>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(supplierService.getAllSuppliers(name, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<SupplierDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplier(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Update supplier")
    public ResponseEntity<SupplierDTO> update(@PathVariable Long id, @RequestBody SupplierDTO dto) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete supplier")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Orders ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/orders")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Place a supply order")
    public ResponseEntity<SupplyOrderDTO> placeOrder(@PathVariable Long id, @RequestBody SupplyOrderDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.placeOrder(id, dto));
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List orders for a supplier")
    public ResponseEntity<List<SupplyOrderDTO>> getOrders(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getOrdersForSupplier(id));
    }

    @PatchMapping("/orders/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Mark order as delivered (triggers stock update)")
    public ResponseEntity<SupplyOrderDTO> markDelivered(@PathVariable Long orderId) {
        return ResponseEntity.ok(supplierService.markDelivered(orderId));
    }

    @PatchMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Cancel a pending order")
    public ResponseEntity<SupplyOrderDTO> cancel(@PathVariable Long orderId) {
        return ResponseEntity.ok(supplierService.cancelOrder(orderId));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Filter orders by status (PENDING | DELIVERED | CANCELLED)")
    public ResponseEntity<List<SupplyOrderDTO>> byStatus(@RequestParam String status) {
        return ResponseEntity.ok(supplierService.getOrdersByStatus(status));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Toggle supplier active/inactive status")
    public ResponseEntity<SupplierDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.toggleStatus(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Get performance stats for a supplier")
    public ResponseEntity<SupplierStatsDTO> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getStats(id));
    }
}