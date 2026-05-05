package tn.esprit.spring.opsservice.dossier.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.dossier.dto.request.CreateDossierEntryRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.SignDossierEntryRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierEntryResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierEntrySignatureResponse;
import tn.esprit.spring.opsservice.dossier.entity.DossierEntry;
import tn.esprit.spring.opsservice.dossier.entity.PatientDossier;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.ContributorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;
import tn.esprit.spring.opsservice.dossier.repository.DossierContributorRepository;
import tn.esprit.spring.opsservice.dossier.repository.DossierEntryRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DossierEntryService {

    private final DossierService dossierService;
    private final DossierEntryRepository dossierEntryRepository;
    private final DossierContributorRepository dossierContributorRepository;
    private final DossierMapper dossierMapper;

    public DossierEntryService(
            DossierService dossierService,
            DossierEntryRepository dossierEntryRepository,
            DossierContributorRepository dossierContributorRepository,
            DossierMapper dossierMapper
    ) {
        this.dossierService = dossierService;
        this.dossierEntryRepository = dossierEntryRepository;
        this.dossierContributorRepository = dossierContributorRepository;
        this.dossierMapper = dossierMapper;
    }

    @Transactional
    public DossierEntryResponse createEntry(UUID dossierId, CreateDossierEntryRequest request, ActorContext actorContext) {
        PatientDossier dossier = dossierService.findDossierOrThrow(dossierId);
        if (dossier.getStatus() == DossierStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dossier already archived");
        }
        authorizeEntryCreation(dossier, actorContext);

        DossierEntry entry = new DossierEntry();
        entry.setDossierId(dossierId);
        entry.setEntryType(request.getEntryType());
        entry.setActorRole(actorContext.actorRole());
        entry.setActorId(actorContext.actorId());
        entry.setActorDisplayName(actorContext.actorDisplayName());
        entry.setTitle(request.getTitle());
        entry.setDetails(request.getDetails());
        entry.setMedicationId(request.getMedicationId());
        entry.setMedicationName(request.getMedicationName());
        entry.setDoseValue(request.getDoseValue());
        entry.setDoseUnit(request.getDoseUnit());
        entry.setRoute(request.getRoute());
        entry.setPatientCondition(request.getPatientCondition());
        entry.setVitalsJson(dossierMapper.toJson(request.getVitals()));
        entry.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now());

        boolean signatureRequired = request.getRequiresSignature() != null
                ? request.getRequiresSignature()
                : request.getEntryType() == DossierEntryType.MEDICATION_ADMIN
                || request.getEntryType() == DossierEntryType.CONDITION_UPDATE;
        entry.setRequiresSignature(signatureRequired);
        entry.setSigned(false);

        DossierEntry saved = dossierEntryRepository.save(entry);
        return dossierMapper.toEntryResponse(saved);
    }

    @Transactional
    public DossierEntrySignatureResponse signEntry(
            UUID dossierId,
            UUID entryId,
            SignDossierEntryRequest request,
            ActorContext actorContext
    ) {
        dossierService.findDossierOrThrow(dossierId);
        DossierEntry entry = dossierEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        if (!entry.getDossierId().equals(dossierId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry does not belong to dossier");
        }
        if (entry.isSigned()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entry already signed");
        }
        if (!entry.isRequiresSignature()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entry does not require signature");
        }
        if (!entry.getActorId().equals(actorContext.actorId()) && actorContext.actorRole() != ActorRole.SYSTEM) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only entry actor can sign this entry");
        }

        LocalDateTime signedAt = request.getSignedAt() != null ? request.getSignedAt() : LocalDateTime.now();
        entry.setSigned(true);
        entry.setSignedAt(signedAt);
        entry.setSignatureType(request.getSignatureType());
        entry.setSignatureHash(signatureHash(request.getSignaturePayload(), actorContext.actorId(), entry.getOccurredAt(), entry.getId()));

        DossierEntry saved = dossierEntryRepository.save(entry);
        DossierEntrySignatureResponse response = new DossierEntrySignatureResponse();
        response.setEntryId(saved.getId());
        response.setSigned(saved.isSigned());
        response.setSignedAt(saved.getSignedAt());
        response.setSignatureType(saved.getSignatureType());
        return response;
    }

    public Page<DossierEntryResponse> listEntries(
            UUID dossierId,
            LocalDateTime from,
            LocalDateTime to,
            DossierEntryType type,
            int page,
            int size
    ) {
        dossierService.findDossierOrThrow(dossierId);

        Pageable pageable = PageRequest.of(page, size);
        Specification<DossierEntry> spec = Specification.where((root, q, cb) -> cb.equal(root.get("dossierId"), dossierId));

        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entryType"), type));
        }

        return dossierEntryRepository.findAll(spec, pageable).map(dossierMapper::toEntryResponse);
    }

    private void authorizeEntryCreation(PatientDossier dossier, ActorContext actorContext) {
        if (actorContext.actorRole() == ActorRole.SYSTEM) {
            return;
        }
        if (actorContext.actorRole() == ActorRole.NURSE) {
            if (!actorContext.actorId().equals(dossier.getAssignedNurseId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nurse is not assigned to this dossier");
            }
            return;
        }
        if (actorContext.actorRole() == ActorRole.DOCTOR) {
            boolean allowed = dossierContributorRepository.existsByDossierIdAndContributorIdAndContributorRoleAndActiveTrue(
                    dossier.getId(),
                    actorContext.actorId(),
                    ContributorRole.DOCTOR
            );
            if (!allowed && !actorContext.actorId().equals(dossier.getPrimaryDoctorId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor is not contributor on this dossier");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Actor role not allowed");
    }

    private String signatureHash(String payload, UUID actorId, LocalDateTime occurredAt, UUID entryId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String content = payload + "|" + actorId + "|" + occurredAt + "|" + entryId;
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
