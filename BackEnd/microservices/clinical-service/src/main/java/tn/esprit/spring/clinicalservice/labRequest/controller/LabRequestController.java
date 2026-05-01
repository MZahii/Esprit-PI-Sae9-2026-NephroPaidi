package tn.esprit.spring.clinicalservice.labRequest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.labRequest.dto.CreateLabRequestRequest;
import tn.esprit.spring.clinicalservice.labRequest.dto.LabRequestDto;
import tn.esprit.spring.clinicalservice.labRequest.dto.UploadLabResultRequest;
import tn.esprit.spring.clinicalservice.labRequest.service.LabRequestService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/lab-requests")
@RequiredArgsConstructor
public class LabRequestController {

    private final LabRequestService labRequestService;
    private final DoctorIdResolver doctorIdResolver;

    @PostMapping
    public ResponseEntity<LabRequestDto> createLabRequest(
            @RequestBody CreateLabRequestRequest request,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication) {
        UUID resolvedDoctorId = doctorIdResolver.resolve(doctorId, authentication);
        LabRequestDto response = labRequestService.createLabRequest(request, resolvedDoctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<LabRequestDto>> getMyLabRequests(
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication) {
        UUID resolvedDoctorId = doctorIdResolver.resolve(doctorId, authentication);
        List<LabRequestDto> requests = labRequestService.getLabRequestsByDoctor(resolvedDoctorId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LabRequestDto>> getPendingLabRequests() {
        List<LabRequestDto> requests = labRequestService.getPendingLabRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabRequestDto> getLabRequest(@PathVariable UUID id) {
        LabRequestDto request = labRequestService.getLabRequestById(id);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/{id}/results")
    public ResponseEntity<LabRequestDto> uploadLabResult(
            @PathVariable UUID id,
            @RequestBody UploadLabResultRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            Authentication authentication) {
        // TODO: Resolve userId from authentication if not provided
        LabRequestDto response = labRequestService.uploadLabResult(id, request, userId);
        return ResponseEntity.ok(response);
    }
}
