package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.surgical.domain.entity.CareTask;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PostOpObservation;
import tn.esprit.spring.procedureservice.surgical.dto.request.AddObservationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateObservationRequest;
import tn.esprit.spring.procedureservice.surgical.repository.CareTaskRepository;
import tn.esprit.spring.procedureservice.surgical.repository.PostOpObservationRepository;

@Service
public class PostOpObservationService {
    private final PostOpObservationRepository repository;
    private final SurgicalCaseService surgicalCaseService;
    private final CareTaskRepository careTaskRepository;

    public PostOpObservationService(
        PostOpObservationRepository repository,
        SurgicalCaseService surgicalCaseService,
        CareTaskRepository careTaskRepository
    ) {
        this.repository = repository;
        this.surgicalCaseService = surgicalCaseService;
        this.careTaskRepository = careTaskRepository;
    }

    public PostOpObservation create(AddObservationRequest request) {
        PostOpObservation observation = new PostOpObservation();
        observation.setSurgicalCase(surgicalCaseService.getById(request.surgicalCaseId()));
        observation.setNotes(request.notes());
        PostOpObservation saved = repository.save(observation);
        applyPostOpWorkflow(request.surgicalCaseId(), request.notes());
        return saved;
    }

    public PostOpObservation update(Long id, UpdateObservationRequest request) {
        PostOpObservation observation = getById(id);
        observation.setNotes(request.notes());
        PostOpObservation saved = repository.save(observation);
        Long caseId = observation.getSurgicalCase() != null ? observation.getSurgicalCase().getId() : null;
        if (caseId != null) {
            applyPostOpWorkflow(caseId, request.notes());
        }
        return saved;
    }

    public PostOpObservation getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Post-op observation not found: " + id));
    }

    public List<PostOpObservation> getAll() {
        return repository.findAll();
    }

    private void applyPostOpWorkflow(Long caseId, String notes) {
        boolean stable = evaluatePostOpStability(notes);
        surgicalCaseService.applyPostOpDecision(caseId, stable);
        if (!stable) {
            ensureTask(caseId, "Monitor hemodynamics every 30 minutes");
            ensureTask(caseId, "Re-evaluate bleeding and wound status");
            ensureTask(caseId, "Notify surgeon on-call and document escalation");
        }
    }

    private void ensureTask(Long surgicalCaseId, String title) {
        if (careTaskRepository.existsBySurgicalCaseIdAndTitle(surgicalCaseId, title)) {
            return;
        }

        CareTask task = new CareTask();
        task.setSurgicalCase(surgicalCaseService.getById(surgicalCaseId));
        task.setTitle(title);
        task.setDone(false);
        careTaskRepository.save(task);
    }

    private boolean evaluatePostOpStability(String notes) {
        return parseBooleanFlag(notes, "hemodynamicsStable")
            && parseBooleanFlag(notes, "bleedingControlled")
            && parseBooleanFlag(notes, "painControlled")
            && parseBooleanFlag(notes, "consciousnessNormal");
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
