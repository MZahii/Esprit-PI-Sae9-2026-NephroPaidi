package tn.esprit.spring.opsservice.dossier.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.opsservice.dossier.dto.request.CreatePatientDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.request.DischargeDossierRequest;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierSummaryResponse;
import tn.esprit.spring.opsservice.dossier.entity.DossierEntry;
import tn.esprit.spring.opsservice.dossier.entity.PatientDossier;
import tn.esprit.spring.opsservice.dossier.model.ActorRole;
import tn.esprit.spring.opsservice.dossier.model.DossierEntryType;
import tn.esprit.spring.opsservice.dossier.model.DossierStatus;
import tn.esprit.spring.opsservice.dossier.repository.DossierEntryRepository;
import tn.esprit.spring.opsservice.dossier.repository.PatientDossierRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DossierService {

    private final PatientDossierRepository patientDossierRepository;
    private final DossierEntryRepository dossierEntryRepository;
    private final NurseAssignmentService nurseAssignmentService;
    private final DossierMapper dossierMapper;

    public DossierService(
            PatientDossierRepository patientDossierRepository,
            DossierEntryRepository dossierEntryRepository,
            NurseAssignmentService nurseAssignmentService,
            DossierMapper dossierMapper
    ) {
        this.patientDossierRepository = patientDossierRepository;
        this.dossierEntryRepository = dossierEntryRepository;
        this.nurseAssignmentService = nurseAssignmentService;
        this.dossierMapper = dossierMapper;
    }

    @Transactional
    public PatientDossierResponse createDossier(CreatePatientDossierRequest request) {
        if (patientDossierRepository.existsByHospitalizationRequestId(request.getHospitalizationRequestId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "hospitalizationRequestId already used");
        }

        UUID assignedNurseId = nurseAssignmentService.autoAssignNurse(request.getPatientId());
        if (assignedNurseId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No nurse available for assignment");
        }

        PatientDossier dossier = new PatientDossier();
        dossier.setPatientId(request.getPatientId());
        dossier.setSourceConsultationId(request.getSourceConsultationId());
        dossier.setSourceAppointmentId(request.getSourceAppointmentId());
        dossier.setHospitalizationRequestId(request.getHospitalizationRequestId());
        dossier.setPrimaryDoctorId(request.getPrimaryDoctorId());
        dossier.setAdmissionReason(request.getAdmissionReason());
        dossier.setAdmissionPriority(request.getAdmissionPriority());
        dossier.setExpectedDischargeAt(request.getExpectedDischargeAt());
        dossier.setAssignedNurseId(assignedNurseId);
        dossier.setStatus(DossierStatus.ACTIVE);

        PatientDossier saved = patientDossierRepository.save(dossier);
        return dossierMapper.toResponse(saved);
    }

    public PatientDossierResponse getById(UUID dossierId) {
        return dossierMapper.toResponse(findDossierOrThrow(dossierId));
    }

    public Page<PatientDossierSummaryResponse> list(
            DossierStatus status,
            Long patientId,
            UUID doctorId,
            UUID nurseId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        List<PatientDossierSummaryResponse> filtered = patientDossierRepository.findAll().stream()
                .filter((dossier) -> status == null || dossier.getStatus() == status)
                .filter((dossier) -> patientId == null || patientId.equals(dossier.getPatientId()))
                .filter((dossier) -> doctorId == null || doctorId.equals(dossier.getPrimaryDoctorId()))
                .filter((dossier) -> nurseId == null || nurseId.equals(dossier.getAssignedNurseId()))
                .map(dossierMapper::toSummary)
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    public Page<PatientDossierSummaryResponse> listMyAssigned(UUID nurseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<PatientDossierSummaryResponse> filtered = patientDossierRepository.findAll().stream()
                .filter((dossier) -> nurseId.equals(dossier.getAssignedNurseId()))
                .filter((dossier) -> dossier.getStatus() == DossierStatus.ACTIVE
                        || dossier.getStatus() == DossierStatus.IN_PROGRESS
                        || dossier.getStatus() == DossierStatus.READY_FOR_DISCHARGE)
                .map(dossierMapper::toSummary)
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional
    public PatientDossierResponse discharge(
            UUID dossierId,
            DischargeDossierRequest request,
            ActorContext actorContext
    ) {
        PatientDossier dossier = findDossierOrThrow(dossierId);
        if (dossier.getStatus() == DossierStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot discharge archived dossier");
        }
        if (request.getDischargedAt().isBefore(dossier.getAdmittedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dischargedAt must be after admittedAt");
        }

        dossier.setStatus(DossierStatus.DISCHARGED);
        dossier.setDischargedAt(request.getDischargedAt());
        dossier.setDischargeSummary(request.getDischargeSummary());
        PatientDossier saved = patientDossierRepository.save(dossier);

        boolean requiresSigned = request.getRequiresSignedDischargeNote() == null || request.getRequiresSignedDischargeNote();
        DossierEntry dischargeEntry = new DossierEntry();
        dischargeEntry.setId(UUID.randomUUID());
        dischargeEntry.setDossierId(saved.getId());
        dischargeEntry.setEntryType(DossierEntryType.DISCHARGE_NOTE);
        dischargeEntry.setActorRole(actorContext.actorRole() == null ? ActorRole.SYSTEM : actorContext.actorRole());
        dischargeEntry.setActorId(actorContext.actorId());
        dischargeEntry.setActorDisplayName(actorContext.actorDisplayName());
        dischargeEntry.setTitle("Patient discharged");
        dischargeEntry.setDetails(request.getDischargeSummary());
        dischargeEntry.setOccurredAt(request.getDischargedAt());
        dischargeEntry.setRequiresSignature(requiresSigned);

        if (requiresSigned) {
            if (request.getSignature() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature is required for discharge note");
            }
            dischargeEntry.setSigned(true);
            dischargeEntry.setSignedAt(LocalDateTime.now());
            dischargeEntry.setSignatureType(request.getSignature().getSignatureType());
            dischargeEntry.setSignatureHash(signatureHash(
                    request.getSignature().getSignaturePayload(),
                    actorContext.actorId(),
                    dischargeEntry.getOccurredAt(),
                    dischargeEntry.getId()
            ));
        }

        dossierEntryRepository.save(dischargeEntry);
        return dossierMapper.toResponse(saved);
    }

    @Transactional
    public PatientDossierResponse archive(UUID dossierId) {
        PatientDossier dossier = findDossierOrThrow(dossierId);
        if (dossier.getStatus() != DossierStatus.DISCHARGED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dossier must be discharged before archive");
        }
        dossier.setStatus(DossierStatus.ARCHIVED);
        dossier.setArchivedToHistory(true);
        dossier.setArchivedAt(LocalDateTime.now());
        return dossierMapper.toResponse(patientDossierRepository.save(dossier));
    }

    public PatientDossier findDossierOrThrow(UUID dossierId) {
        return patientDossierRepository.findById(dossierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier not found"));
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
