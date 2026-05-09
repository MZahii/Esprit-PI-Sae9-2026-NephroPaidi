package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;
import tn.esprit.spring.procedureservice.surgical.dto.request.AddObservationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateObservationRequest;
import tn.esprit.spring.procedureservice.surgical.repository.PreOpAssessmentRepository;

@Service
public class PreOpAssessmentService {
    private final PreOpAssessmentRepository repository;
    private final SurgicalCaseService surgicalCaseService;

    public PreOpAssessmentService(PreOpAssessmentRepository repository, SurgicalCaseService surgicalCaseService) {
        this.repository = repository;
        this.surgicalCaseService = surgicalCaseService;
    }

    public PreOpAssessment create(AddObservationRequest request) {
        PreOpAssessment assessment = new PreOpAssessment();
        assessment.setSurgicalCase(surgicalCaseService.getById(request.surgicalCaseId()));
        assessment.setNotes(request.notes());
        PreOpAssessment saved = repository.save(assessment);
        surgicalCaseService.applyPreOpDecision(request.surgicalCaseId(), evaluatePreOpEligibility(request.notes()));
        return saved;
    }

    public PreOpAssessment update(Long id, UpdateObservationRequest request) {
        PreOpAssessment assessment = getById(id);
        assessment.setNotes(request.notes());
        PreOpAssessment saved = repository.save(assessment);
        Long caseId = assessment.getSurgicalCase() != null ? assessment.getSurgicalCase().getId() : null;
        if (caseId != null) {
            surgicalCaseService.applyPreOpDecision(caseId, evaluatePreOpEligibility(request.notes()));
        }
        return saved;
    }

    public PreOpAssessment getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Pre-op assessment not found: " + id));
    }

    public List<PreOpAssessment> getAll() {
        return repository.findAll();
    }

    private boolean evaluatePreOpEligibility(String notes) {
        return parseBooleanFlag(notes, "hemodynamicsOk")
            && parseBooleanFlag(notes, "infectionScreenOk")
            && parseBooleanFlag(notes, "anesthesiaClearanceOk")
            && parseBooleanFlag(notes, "consentSigned");
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
