package tn.esprit.spring.clinicalservice.discharge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.discharge.dto.CreateDischargeFollowUpRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.CreateFollowUpItemRequest;
import tn.esprit.spring.clinicalservice.discharge.dto.DischargeFollowUpDto;
import tn.esprit.spring.clinicalservice.discharge.service.DischargeFollowUpService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/discharge-follow-ups")
@RequiredArgsConstructor
@Slf4j
public class DischargeFollowUpController {

    private final DischargeFollowUpService dischargeFollowUpService;
    private final DoctorIdResolver doctorIdResolver;

    @PostMapping
    public ResponseEntity<DischargeFollowUpDto> createFollowUp(
            @RequestBody CreateDischargeFollowUpRequest request,
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
        
        log.info("Creating discharge follow-up for patient: {}", request.getPatientId());

        DischargeFollowUpDto response = dischargeFollowUpService.createFollowUp(request, resolvedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DischargeFollowUpDto>> getFollowUpsByPatient(@PathVariable Long patientId) {
        log.info("Fetching follow-ups for patient: {}", patientId);

        List<DischargeFollowUpDto> followUps = dischargeFollowUpService.getFollowUpsByPatient(patientId);
        return ResponseEntity.ok(followUps);
    }

    @GetMapping("/doctor/my")
    public ResponseEntity<List<DischargeFollowUpDto>> getFollowUpsByDoctor(
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
        
        log.info("Fetching follow-ups created by doctor: {}", resolvedId);

        List<DischargeFollowUpDto> followUps = dischargeFollowUpService.getFollowUpsByDoctor(resolvedId);
        return ResponseEntity.ok(followUps);
    }

    @PostMapping("/{followUpId}/items")
    public ResponseEntity<DischargeFollowUpDto> addFollowUpItem(
            @PathVariable UUID followUpId,
            @RequestBody CreateFollowUpItemRequest itemRequest) {
        log.info("Adding item to follow-up: {}", followUpId);

        DischargeFollowUpDto response = dischargeFollowUpService.addFollowUpItem(followUpId, itemRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/items/{itemId}/status")
    public ResponseEntity<DischargeFollowUpDto> updateItemStatus(
            @PathVariable UUID itemId,
            @RequestParam String status) {
        log.info("Updating follow-up item {} status to {}", itemId, status);

        DischargeFollowUpDto response = dischargeFollowUpService.updateItemStatus(itemId, status);
        return ResponseEntity.ok(response);
    }
}
