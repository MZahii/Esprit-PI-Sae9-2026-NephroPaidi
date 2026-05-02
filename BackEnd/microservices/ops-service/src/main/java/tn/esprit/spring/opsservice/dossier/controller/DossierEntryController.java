package tn.esprit.spring.opsservice.dossier.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.opsservice.common.dto.PagedResponse;
import tn.esprit.spring.opsservice.dossier.dto.request.CreateDossierEntryRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.SignDossierEntryRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierEntryResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierEntrySignatureResponse;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;
import tn.esprit.spring.opsservice.dossier.service.ActorContext;
import tn.esprit.spring.opsservice.dossier.service.DossierEntryService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/ops/dossiers/{dossierId}/entries")
public class DossierEntryController {

    private final DossierEntryService dossierEntryService;

    public DossierEntryController(DossierEntryService dossierEntryService) {
        this.dossierEntryService = dossierEntryService;
    }

    @PostMapping
    public ResponseEntity<DossierEntryResponse> create(
            @PathVariable UUID dossierId,
            @Valid @RequestBody CreateDossierEntryRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") ActorRole role,
            @RequestHeader(value = "X-User-Display-Name", required = false) String displayName
    ) {
        ActorContext actorContext = new ActorContext(
                userId,
                role,
                displayName == null || displayName.isBlank() ? userId.toString() : displayName
        );
        DossierEntryResponse response = dossierEntryService.createEntry(dossierId, request, actorContext);
        return ResponseEntity.created(URI.create("/api/ops/dossiers/" + dossierId + "/entries/" + response.getId())).body(response);
    }

    @PostMapping("/{entryId}/sign")
    public ResponseEntity<DossierEntrySignatureResponse> sign(
            @PathVariable UUID dossierId,
            @PathVariable UUID entryId,
            @Valid @RequestBody SignDossierEntryRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") ActorRole role,
            @RequestHeader(value = "X-User-Display-Name", required = false) String displayName
    ) {
        ActorContext actorContext = new ActorContext(
                userId,
                role,
                displayName == null || displayName.isBlank() ? userId.toString() : displayName
        );
        return ResponseEntity.ok(dossierEntryService.signEntry(dossierId, entryId, request, actorContext));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DossierEntryResponse>> list(
            @PathVariable UUID dossierId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) DossierEntryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Page<DossierEntryResponse> result = dossierEntryService.listEntries(dossierId, from, to, type, page, size);
        return ResponseEntity.ok(PagedResponse.of(result));
    }
}
