package tn.esprit.spring.opsservice.dossier.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.common.dto.PagedResponse;
import tn.esprit.spring.opsservice.dossier.dto.request.AddDossierContributorRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.CreatePatientDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.DischargeDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierContributorResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierSummaryResponse;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;
import tn.esprit.spring.opsservice.dossier.service.ActorContext;
import tn.esprit.spring.opsservice.dossier.service.DossierContributorService;
import tn.esprit.spring.opsservice.dossier.service.DossierService;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/ops/dossiers")
public class PatientDossierController {

    private final DossierService dossierService;
    private final DossierContributorService dossierContributorService;

    public PatientDossierController(
            DossierService dossierService,
            DossierContributorService dossierContributorService
    ) {
        this.dossierService = dossierService;
        this.dossierContributorService = dossierContributorService;
    }

    @PostMapping
    public ResponseEntity<PatientDossierResponse> create(@Valid @RequestBody CreatePatientDossierRequest request) {
        PatientDossierResponse response = dossierService.createDossier(request);
        return ResponseEntity.created(URI.create("/api/ops/dossiers/" + response.getId())).body(response);
    }

    @GetMapping("/{dossierId}")
    public ResponseEntity<PatientDossierResponse> getById(@PathVariable UUID dossierId) {
        return ResponseEntity.ok(dossierService.getById(dossierId));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PatientDossierSummaryResponse>> list(
            @RequestParam(required = false) DossierStatus status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID nurseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PatientDossierSummaryResponse> result = dossierService.list(status, patientId, doctorId, nurseId, page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }

    @GetMapping("/my-assigned")
    public ResponseEntity<PagedResponse<PatientDossierSummaryResponse>> myAssigned(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID resolvedUserId = resolveUserId(userId, actorId);
        Page<PatientDossierSummaryResponse> result = dossierService.listMyAssigned(resolvedUserId, page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }

    private UUID resolveUserId(String userIdHeader, String actorIdHeader) {
        String raw = userIdHeader;
        if (raw == null || raw.isBlank()) {
            raw = actorIdHeader;
        }

        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required header 'X-User-Id'");
        }

        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header 'X-User-Id' must be a valid UUID");
        }
    }

    @PostMapping("/{dossierId}/contributors")
    public ResponseEntity<DossierContributorResponse> addContributor(
            @PathVariable UUID dossierId,
            @Valid @RequestBody AddDossierContributorRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        DossierContributorResponse response = dossierContributorService.addContributor(dossierId, request, userId);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/{dossierId}/contributors/{contributorId}")
    public ResponseEntity<Void> removeContributor(
            @PathVariable UUID dossierId,
            @PathVariable UUID contributorId,
            @RequestParam ContributorRole role
    ) {
        dossierContributorService.removeContributor(dossierId, contributorId, role);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{dossierId}/discharge")
    public ResponseEntity<PatientDossierResponse> discharge(
            @PathVariable UUID dossierId,
            @Valid @RequestBody DischargeDossierRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Role", required = false) ActorRole role,
            @RequestHeader(value = "X-User-Display-Name", required = false) String displayName
    ) {
        ActorContext actorContext = new ActorContext(
                userId,
                role == null ? ActorRole.SYSTEM : role,
                displayName == null || displayName.isBlank() ? userId.toString() : displayName
        );
        return ResponseEntity.ok(dossierService.discharge(dossierId, request, actorContext));
    }

    @PutMapping("/{dossierId}/archive")
    public ResponseEntity<PatientDossierResponse> archive(@PathVariable UUID dossierId) {
        return ResponseEntity.ok(dossierService.archive(dossierId));
    }
}
