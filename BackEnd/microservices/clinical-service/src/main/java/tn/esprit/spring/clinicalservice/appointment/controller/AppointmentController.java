package tn.esprit.spring.clinicalservice.appointment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.appointment.dto.*;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;
import tn.esprit.spring.clinicalservice.appointment.service.AppointmentService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorIdResolver doctorIdResolver;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentResponse response = appointmentService.create(request);
        return ResponseEntity
                .created(URI.create("/clinical/appointments/" + response.getId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> list(
            @RequestParam(value = "doctorId", required = false) UUID doctorId,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "status", required = false) AppointmentStatus status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(appointmentService.list(doctorId, patientId, status, from, to));
    }

    @GetMapping("/availability")
    public ResponseEntity<AppointmentAvailabilityResponse> checkAvailability(
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(appointmentService.checkAvailability(doctorId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentUpdateRequest request
    ) {
        return ResponseEntity.ok(appointmentService.update(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) AppointmentCancelRequest request
    ) {
        return ResponseEntity.ok(appointmentService.cancel(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start-consultation")
    public ResponseEntity<StartConsultationResponse> startConsultation(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        return ResponseEntity.ok(appointmentService.startConsultation(id, resolvedDoctorId));
    }

    private UUID requireDoctorId(UUID doctorId, Authentication authentication) {
        UUID resolved = doctorIdResolver.resolve(doctorId, authentication);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctorId is required");
        }
        return resolved;
    }
}
