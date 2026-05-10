package tn.esprit.spring.clinicalservice.consultation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.*;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.medication.MedicationOrderService;
import tn.esprit.spring.clinicalservice.consultation.metrics.ConsultationMetricsService;
import tn.esprit.spring.clinicalservice.consultation.metrics.FollowUpSuggestionService;
import tn.esprit.spring.clinicalservice.consultation.section.ConsultationSectionService;
import tn.esprit.spring.clinicalservice.consultation.section.ConsultationSectionType;
import tn.esprit.spring.clinicalservice.consultation.service.ConsultationOutcomeService;
import tn.esprit.spring.clinicalservice.consultation.service.ConsultationService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController("doctorConsultationController")
@RequestMapping("/clinical/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationOutcomeService consultationOutcomeService;
    private final ConsultationMetricsService consultationMetricsService;
    private final MedicationOrderService medicationOrderService;
    private final FollowUpSuggestionService followUpSuggestionService;
    private final ConsultationSectionService consultationSectionService;
    private final DoctorIdResolver doctorIdResolver;

    // LOCAL TESTING: pass X-Doctor-Id header
    @PostMapping
    public ResponseEntity<ConsultationResponse> create(
            @Valid @RequestBody ConsultationCreateRequest request,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {

        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        ConsultationResponse response = consultationService.create(request, resolvedDoctorId);

        return ResponseEntity
                .created(URI.create("/clinical/consultations/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(
                consultationService.getById(id, resolvedDoctorId)
        );
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ConsultationResponse>> mine(
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            @RequestParam(value = "patientQuery", required = false) String patientQuery,
            @RequestParam(value = "status", required = false) ConsultationStatus status,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(
                consultationService.listMine(resolvedDoctorId, patientQuery, status)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultationResponse> update(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @Valid @RequestBody ConsultationUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(
                consultationService.update(id, resolvedDoctorId, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        consultationService.cancel(id, resolvedDoctorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/outcomes")
    public ResponseEntity<ConsultationOutcomeResponse> getOutcomes(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.getOutcome(id, resolvedDoctorId));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ConsultationOutcomeResponse> updateNotes(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationOutcomeUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.updateNotes(id, resolvedDoctorId, request != null ? request.getContent() : null));
    }

    @PostMapping("/{id}/diagnosis")
    public ResponseEntity<ConsultationOutcomeResponse> updateDiagnosis(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationOutcomeUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.updateDiagnosis(id, resolvedDoctorId, request != null ? request.getContent() : null));
    }

    @PostMapping("/{id}/prescriptions")
    public ResponseEntity<ConsultationOutcomeResponse> updatePrescriptions(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationOutcomeUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.updatePrescriptions(id, resolvedDoctorId, request != null ? request.getContent() : null));
    }

    @PostMapping("/{id}/lab-requests")
    public ResponseEntity<ConsultationOutcomeResponse> updateLabRequests(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationOutcomeUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.updateLabRequests(id, resolvedDoctorId, request != null ? request.getContent() : null));
    }

    @PostMapping("/{id}/treatment-plan")
    public ResponseEntity<ConsultationOutcomeResponse> updateTreatmentPlan(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationOutcomeUpdateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationOutcomeService.updateTreatmentPlan(id, resolvedDoctorId, request != null ? request.getContent() : null));
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<ConsultationMetricsResponse> getMetrics(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationMetricsService.get(id, resolvedDoctorId));
    }

    @PostMapping("/{id}/metrics")
    public ResponseEntity<ConsultationMetricsResponse> upsertMetrics(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationMetricsRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationMetricsService.upsert(id, resolvedDoctorId, request));
    }

    @GetMapping("/{id}/follow-up-suggestion")
    public ResponseEntity<FollowUpSuggestionResponse> followUpSuggestion(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(followUpSuggestionService.suggest(id, resolvedDoctorId));
    }

    @GetMapping("/{id}/medications")
    public ResponseEntity<List<MedicationOrderResponse>> listMedications(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(medicationOrderService.list(id, resolvedDoctorId));
    }

    @PostMapping("/{id}/medications")
    public ResponseEntity<MedicationOrderResponse> createMedication(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody MedicationOrderRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(medicationOrderService.create(id, resolvedDoctorId, request));
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<List<ConsultationSectionResponse>> listSections(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(consultationSectionService.list(id, resolvedDoctorId));
    }

    @PutMapping("/{id}/sections/{type}")
    public ResponseEntity<ConsultationSectionResponse> upsertSection(
            @PathVariable UUID id,
            @PathVariable String type,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @RequestBody ConsultationSectionRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        ConsultationSectionType sectionType;
        try {
            sectionType = ConsultationSectionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section type: " + type);
        }
        return ResponseEntity.ok(consultationSectionService.upsert(id, resolvedDoctorId, sectionType, request));
    }

    private UUID requireDoctorId(UUID doctorId, Authentication authentication) {
        UUID resolved = doctorIdResolver.resolve(doctorId, authentication);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctorId is required");
        }
        return resolved;
    }

    // BACKOFFICE: List all consultations with filters (no doctor restriction for admin)
    @GetMapping("/backoffice/list")
    public ResponseEntity<List<ConsultationResponse>> listAllConsultations(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "status", required = false) ConsultationStatus status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(consultationService.listAll(patientId, status, from, to));
    }
}
