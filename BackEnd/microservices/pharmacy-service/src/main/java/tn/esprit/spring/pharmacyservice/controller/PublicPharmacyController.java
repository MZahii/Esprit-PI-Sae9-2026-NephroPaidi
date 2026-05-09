package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.BatchDTO;
import tn.esprit.spring.pharmacyservice.dto.MedicationDTO;
import tn.esprit.spring.pharmacyservice.dto.StockDTO;
import tn.esprit.spring.pharmacyservice.service.MedicationService;
import tn.esprit.spring.pharmacyservice.service.StockService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only endpoints for the patient/guardian frontoffice.
 * No authentication required — only safe GET operations exposed.
 */
@RestController
@RequestMapping("/api/public/pharmacy")
@RequiredArgsConstructor
@Tag(name = "Public Pharmacy", description = "Read-only pharmacy info for the guardian portal")
public class PublicPharmacyController {

    private final MedicationService medicationService;
    private final StockService stockService;

    @GetMapping("/medications")
    @Operation(summary = "List all medications (public)")
    public ResponseEntity<List<MedicationDTO>> getMedications() {
        return ResponseEntity.ok(medicationService.getAllMedications());
    }

    @GetMapping("/medications/{id}/batches")
    @Operation(summary = "List non-expired batches for a medication (public)")
    public ResponseEntity<List<BatchDTO>> getBatches(@PathVariable Long id) {
        return ResponseEntity.ok(medicationService.getBatchesForMedication(id));
    }

    @GetMapping("/stock")
    @Operation(summary = "List all stock entries (public)")
    public ResponseEntity<List<StockDTO>> getStock() {
        return ResponseEntity.ok(stockService.getAllStock());
    }
}