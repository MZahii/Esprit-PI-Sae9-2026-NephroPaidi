package tn.esprit.spring.clinicalservice.surgery.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.surgery.dto.CreateSurgeryIndicationRequest;
import tn.esprit.spring.clinicalservice.surgery.dto.SurgeryIndicationDto;
import tn.esprit.spring.clinicalservice.surgery.service.SurgeryIndicationService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/surgery-indications")
@RequiredArgsConstructor
@Slf4j
public class SurgeryIndicationController {

    private final SurgeryIndicationService surgeryIndicationService;
    private final DoctorIdResolver doctorIdResolver;

    @PostMapping
    public ResponseEntity<SurgeryIndicationDto> createSurgeryIndication(
            @RequestBody CreateSurgeryIndicationRequest request,
            @RequestHeader(value = "X-Doctor-Id", required = false) String doctorIdHeader,
            Authentication authentication) {
        UUID doctorId = null;
        try {
            if (doctorIdHeader != null && !doctorIdHeader.isEmpty()) {
                doctorId = UUID.fromString(doctorIdHeader);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid UUID format
        }
        
        UUID resolvedId = doctorIdResolver.resolve(doctorId, authentication);
        if (resolvedId == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("Creating surgery indication for doctor: {} patient: {}", resolvedId, request.getPatientId());

        SurgeryIndicationDto response = surgeryIndicationService.createSurgeryIndication(request, resolvedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SurgeryIndicationDto>> getPendingSurgeryIndications() {
        log.info("Fetching pending surgery indications");

        List<SurgeryIndicationDto> indications = surgeryIndicationService.getPendingSurgeryIndications();
        return ResponseEntity.ok(indications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurgeryIndicationDto> getSurgeryIndicationById(@PathVariable UUID id) {
        log.info("Fetching surgery indication: {}", id);

        SurgeryIndicationDto indication = surgeryIndicationService.getSurgeryIndicationById(id);
        return ResponseEntity.ok(indication);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SurgeryIndicationDto> updateSurgeryIndicationStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        log.info("Updating surgery indication {} status to {}", id, status);

        SurgeryIndicationDto updated = surgeryIndicationService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
