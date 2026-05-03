package tn.esprit.spring.clinicalservice.followup.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.followup.dto.DoctorFollowUpConfirmRequest;
import tn.esprit.spring.clinicalservice.followup.dto.DoctorFollowUpCreateRequest;
import tn.esprit.spring.clinicalservice.followup.dto.DoctorFollowUpResponse;
import tn.esprit.spring.clinicalservice.followup.service.DoctorFollowUpRequestService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DoctorFollowUpController {

    private final DoctorFollowUpRequestService doctorFollowUpRequestService;
    private final DoctorIdResolver doctorIdResolver;

    @PostMapping("/clinical/consultations/{consultationId}/follow-up-requests")
    public ResponseEntity<DoctorFollowUpResponse> create(
            @PathVariable UUID consultationId,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication,
            @Valid @RequestBody DoctorFollowUpCreateRequest request
    ) {
        UUID resolvedDoctorId = requireDoctorId(doctorId, authentication);
        DoctorFollowUpResponse body = doctorFollowUpRequestService.createFromConsultation(
                consultationId, resolvedDoctorId, request);
        return ResponseEntity
                .created(URI.create("/clinical/follow-up-requests/" + body.getId()))
                .body(body);
    }

    @GetMapping("/clinical/follow-up-requests")
    public ResponseEntity<List<DoctorFollowUpResponse>> listPending() {
        return ResponseEntity.ok(doctorFollowUpRequestService.listPending());
    }

    @PostMapping("/clinical/follow-up-requests/{id}/confirm")
    public ResponseEntity<DoctorFollowUpResponse> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorFollowUpConfirmRequest request
    ) {
        return ResponseEntity.ok(doctorFollowUpRequestService.confirm(id, request));
    }

    private UUID requireDoctorId(UUID doctorId, Authentication authentication) {
        UUID resolved = doctorIdResolver.resolve(doctorId, authentication);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "doctorId is required");
        }
        return resolved;
    }
}
