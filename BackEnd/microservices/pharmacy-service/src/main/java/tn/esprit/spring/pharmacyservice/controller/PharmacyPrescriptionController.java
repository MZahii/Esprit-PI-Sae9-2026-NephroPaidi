package tn.esprit.spring.pharmacyservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.pharmacyservice.dto.PharmacyPrescriptionDTO;
import tn.esprit.spring.pharmacyservice.service.PharmacyPrescriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Pharmacy Prescriptions", description = "Doctor-to-pharmacy prescription flow")
public class PharmacyPrescriptionController {

    private final PharmacyPrescriptionService service;

    /** Doctor calls this to send prescription to pharmacy */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','PHARMACIST','ADMIN')")
    @Operation(summary = "Submit a prescription to the pharmacy (called by doctor or clinical system)")
    public ResponseEntity<PharmacyPrescriptionDTO> receive(@RequestBody PharmacyPrescriptionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "List all prescriptions received (pharmacist view)")
    public ResponseEntity<List<PharmacyPrescriptionDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Filter prescriptions by status (PENDING, PROCESSING, DISPENSED, CANCELLED)")
    public ResponseEntity<List<PharmacyPrescriptionDTO>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','DOCTOR')")
    @Operation(summary = "Get prescriptions for a specific patient")
    public ResponseEntity<List<PharmacyPrescriptionDTO>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getByPatient(patientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN','DOCTOR')")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<PharmacyPrescriptionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Update prescription status (pharmacist processes it)")
    public ResponseEntity<PharmacyPrescriptionDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String processedBy) {
        return ResponseEntity.ok(service.updateStatus(id, status, processedBy));
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
    @Operation(summary = "Pharmacist verifies prescription: allergy check + dose validation")
    public ResponseEntity<PharmacyPrescriptionDTO> verify(
            @PathVariable Long id,
            @RequestParam String verifiedBy,
            @RequestParam(defaultValue = "true") boolean allergyConfirmed) {
        return ResponseEntity.ok(service.verify(id, verifiedBy, allergyConfirmed));
    }
}