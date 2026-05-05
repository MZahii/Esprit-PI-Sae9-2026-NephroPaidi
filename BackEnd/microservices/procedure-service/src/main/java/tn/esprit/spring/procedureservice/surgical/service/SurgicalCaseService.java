package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.notification.service.ResendEmailService;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalCase;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.DecideTransplantOfferRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.repository.PreOpAssessmentRepository;
import tn.esprit.spring.procedureservice.surgical.repository.SurgicalCaseRepository;

@Service
public class SurgicalCaseService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
        "OPEN",
        "READY_FOR_INTERVENTION",
        "BLOCKED_PREOP",
        "IN_PROGRESS",
        "POSTOP_STABLE",
        "POSTOP_UNSTABLE",
        "DONE",
        "CANCELLED",
        "ARCHIVED"
    );
    private static final Set<String> ALLOWED_OFFER_STATUSES = Set.of("PENDING", "ACCEPTED", "REJECTED");
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        "OPEN", Set.of("OPEN", "READY_FOR_INTERVENTION", "BLOCKED_PREOP", "CANCELLED"),
        "READY_FOR_INTERVENTION", Set.of("READY_FOR_INTERVENTION", "IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS", Set.of("IN_PROGRESS", "POSTOP_STABLE", "POSTOP_UNSTABLE", "DONE", "CANCELLED"),
        "POSTOP_STABLE", Set.of("POSTOP_STABLE", "DONE", "ARCHIVED"),
        "POSTOP_UNSTABLE", Set.of("POSTOP_UNSTABLE", "ARCHIVED"),
        "BLOCKED_PREOP", Set.of("BLOCKED_PREOP", "ARCHIVED"),
        "DONE", Set.of("DONE", "ARCHIVED"),
        "CANCELLED", Set.of("CANCELLED", "ARCHIVED"),
        "ARCHIVED", Set.of("ARCHIVED")
    );

    private final SurgicalCaseRepository repository;
    private final PreOpAssessmentRepository preOpAssessmentRepository;
    private final ResendEmailService resendEmailService;

    public SurgicalCaseService(
        SurgicalCaseRepository repository,
        PreOpAssessmentRepository preOpAssessmentRepository,
        ResendEmailService resendEmailService
    ) {
        this.repository = repository;
        this.preOpAssessmentRepository = preOpAssessmentRepository;
        this.resendEmailService = resendEmailService;
    }

    public SurgicalCase create(CreateSurgicalCaseRequest request) {
        SurgicalCase surgicalCase = new SurgicalCase();
        surgicalCase.setPatientId(request.patientId());
        surgicalCase.setConsultationId(request.consultationId());
        surgicalCase.setAppointmentId(request.appointmentId());
        surgicalCase.setFirstName(request.firstName());
        surgicalCase.setLastName(request.lastName());
        surgicalCase.setAge(request.age());
        surgicalCase.setGender(request.gender());
        surgicalCase.setMedicalRecordNumber(request.medicalRecordNumber());
        surgicalCase.setSurgeryType(request.surgeryType());
        surgicalCase.setProcedureName(request.procedureName());
        surgicalCase.setSurgeryCategory(request.surgeryCategory());
        surgicalCase.setUrgencyLevel(request.urgencyLevel());
        surgicalCase.setSurgeonId(request.surgeonId());
        surgicalCase.setAssistantSurgeonId(request.assistantSurgeonId());
        surgicalCase.setAnesthesiologistId(request.anesthesiologistId());
        surgicalCase.setNurseTeam(request.nurseTeam());
        surgicalCase.setScheduledDate(request.scheduledDate());
        surgicalCase.setScheduledStartTime(request.scheduledStartTime());
        surgicalCase.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        surgicalCase.setOperatingRoom(request.operatingRoom());
        surgicalCase.setStatus("OPEN");
        surgicalCase.setOfferStatus("PENDING");
        SurgicalCase saved = repository.save(surgicalCase);
        resendEmailService.sendSurgicalCaseCreatedNotification(saved);
        return saved;
    }

    public SurgicalCase update(Long id, UpdateSurgicalCaseRequest request) {
        SurgicalCase surgicalCase = getById(id);
        String requestedStatus = normalizeStatus(request.status());
        if (!ALLOWED_STATUSES.contains(requestedStatus)) {
            throw new BusinessException("Invalid surgical case status: " + requestedStatus);
        }

        if (isLockedStatus(surgicalCase.getStatus())
            && !"ARCHIVED".equals(requestedStatus)
            && !surgicalCase.getStatus().equals(requestedStatus)) {
            throw new BusinessException("Case is " + surgicalCase.getStatus() + ". Status is locked and cannot be updated.");
        }

        if ("IN_PROGRESS".equals(requestedStatus) && !isLatestPreOpEligible(id)) {
            throw new BusinessException("Cannot start surgery: latest Pre-Op decision is BLOCKED.");
        }

        validateStatusTransition(surgicalCase.getStatus(), requestedStatus);

        surgicalCase.setStatus(requestedStatus);
        return repository.save(surgicalCase);
    }

    public SurgicalCase decideOffer(Long id, DecideTransplantOfferRequest request) {
        SurgicalCase surgicalCase = getById(id);
        String offerStatus = normalizeStatus(request.offerStatus());
        if (!ALLOWED_OFFER_STATUSES.contains(offerStatus)) {
            throw new BusinessException("Invalid transplant offer status: " + offerStatus);
        }
        surgicalCase.setOfferStatus(offerStatus);
        return repository.save(surgicalCase);
    }

    public SurgicalCase getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Surgical case not found: " + id));
    }

    public List<SurgicalCase> getAll() {
        return repository.findAll();
    }

    public SurgicalCase applyPreOpDecision(Long caseId, boolean eligible) {
        SurgicalCase surgicalCase = getById(caseId);
        if (isLockedStatus(surgicalCase.getStatus())) {
            return surgicalCase;
        }
        surgicalCase.setStatus(eligible ? "READY_FOR_INTERVENTION" : "BLOCKED_PREOP");
        return repository.save(surgicalCase);
    }

    public SurgicalCase applyPostOpDecision(Long caseId, boolean stable) {
        SurgicalCase surgicalCase = getById(caseId);
        if (isLockedStatus(surgicalCase.getStatus())) {
            return surgicalCase;
        }
        surgicalCase.setStatus(stable ? "POSTOP_STABLE" : "POSTOP_UNSTABLE");
        return repository.save(surgicalCase);
    }

    private boolean isLatestPreOpEligible(Long caseId) {
        Optional<PreOpAssessment> latest = preOpAssessmentRepository.findTopBySurgicalCaseIdOrderByIdDesc(caseId);
        if (latest.isEmpty()) {
            return false;
        }

        String notes = latest.get().getNotes();
        return parseBooleanFlag(notes, "hemodynamicsOk")
            && parseBooleanFlag(notes, "infectionScreenOk")
            && parseBooleanFlag(notes, "anesthesiaClearanceOk")
            && parseBooleanFlag(notes, "consentSigned");
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private boolean isLockedStatus(String status) {
        return "BLOCKED_PREOP".equals(status) || "POSTOP_UNSTABLE".equals(status);
    }

    private void validateStatusTransition(String currentStatus, String requestedStatus) {
        Set<String> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of(currentStatus));
        if (!allowedTargets.contains(requestedStatus)) {
            throw new BusinessException(
                "Invalid transition from " + currentStatus + " to " + requestedStatus + "."
            );
        }
    }

    private boolean parseBooleanFlag(String notes, String key) {
        if (notes == null || notes.isBlank()) {
            return false;
        }

        String prefix = key + "=";
        for (String part : notes.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                return "true".equalsIgnoreCase(value);
            }
        }
        return false;
    }
}
