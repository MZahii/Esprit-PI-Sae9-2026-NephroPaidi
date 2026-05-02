package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.BatchDTO;
import tn.esprit.spring.pharmacyservice.dto.MedicationDTO;
import tn.esprit.spring.pharmacyservice.dto.ReorderAlertDTO;
import tn.esprit.spring.pharmacyservice.service.EmailService;
import tn.esprit.spring.pharmacyservice.service.MedicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
@Tag(name = "Medications", description = "Medication reference & batch management")
public class MedicationController {

    private final MedicationService medicationService;
    private final EmailService emailService;

    // ─── Medications ──────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Create a new medication")
    public ResponseEntity<MedicationDTO> create(@RequestBody MedicationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.createMedication(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','DOCTOR','NURSE','GUARDIAN')")
    @Operation(summary = "List medications with optional filters",
               description = "Supports ?name=, ?form=, ?sort=az|za")
    public ResponseEntity<List<MedicationDTO>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(medicationService.getAllMedications(name, form, sort));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','DOCTOR','NURSE','GUARDIAN')")
    @Operation(summary = "Get medication by ID")
    public ResponseEntity<MedicationDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(medicationService.getMedication(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Update medication")
    public ResponseEntity<MedicationDTO> update(@PathVariable Long id, @RequestBody MedicationDTO dto) {
        return ResponseEntity.ok(medicationService.updateMedication(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete medication")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        medicationService.deleteMedication(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Batches ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/batches")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Add a batch to medication")
    public ResponseEntity<BatchDTO> addBatch(@PathVariable Long id, @RequestBody BatchDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.addBatch(id, dto));
    }

    @GetMapping("/{id}/batches")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','DOCTOR','NURSE','GUARDIAN')")
    @Operation(summary = "List batches for a medication")
    public ResponseEntity<List<BatchDTO>> getBatches(@PathVariable Long id) {
        return ResponseEntity.ok(medicationService.getBatchesForMedication(id));
    }

    @GetMapping("/batches/expired")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List all expired batches")
    public ResponseEntity<List<BatchDTO>> getExpired() {
        return ResponseEntity.ok(medicationService.getExpiredBatches());
    }

    @GetMapping("/batches/expiring-soon")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List batches expiring within N days")
    public ResponseEntity<List<BatchDTO>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(medicationService.getBatchesExpiringSoon(days));
    }

    @GetMapping("/reorder-needed")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List medications whose stock is below the configured minimum threshold")
    public ResponseEntity<List<ReorderAlertDTO>> getReorderNeeded() {
        return ResponseEntity.ok(medicationService.getMedicationsNeedingReorder());
    }

    @PostMapping("/reorder-needed/send-alert")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Send a low-stock alert email for all medications below minimum threshold")
    public ResponseEntity<Void> sendLowStockAlert(@RequestParam String recipientEmail) {
        List<ReorderAlertDTO> alerts = medicationService.getMedicationsNeedingReorder();
        if (alerts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        emailService.sendLowStockAlertEmail(alerts, recipientEmail);
        return ResponseEntity.ok().build();
    }
}