package tn.esprit.spring.procedureservice.dialysis.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisOutcome;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;
import tn.esprit.spring.procedureservice.dialysis.dto.request.ValidateDialysisOutcomeRequest;
import tn.esprit.spring.procedureservice.dialysis.repository.DialysisOutcomeRepository;
import tn.esprit.spring.procedureservice.shared.exception.BusinessException;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;

@Service
public class DialysisOutcomeService {
    private final DialysisOutcomeRepository repository;
    private final DialysisSessionService sessionService;

    public DialysisOutcomeService(DialysisOutcomeRepository repository, DialysisSessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    public DialysisOutcome createForSession(Long sessionId) {
        if (repository.existsBySessionId(sessionId)) {
            throw new BusinessException("A dialysis outcome already exists for session " + sessionId + ".");
        }

        DialysisSession session = sessionService.getById(sessionId);
        String planStatus = session.getPlan() != null && session.getPlan().getStatus() != null
            ? session.getPlan().getStatus().trim().toUpperCase()
            : "";
        if ("CANCELLED".equals(planStatus) || "ARCHIVED".equals(planStatus)) {
            throw new BusinessException("Cannot create an outcome for a " + planStatus + " dialysis plan.");
        }

        DialysisOutcome outcome = new DialysisOutcome();
        outcome.setSession(session);
        outcome.setValidated(false);
        return repository.save(outcome);
    }

    public DialysisOutcome validate(Long id, ValidateDialysisOutcomeRequest request) {
        DialysisOutcome outcome = getById(id);
        String summary = request.summary() == null ? "" : request.summary().trim();
        if (request.validated() && summary.length() < 5) {
            throw new BusinessException("Outcome summary must contain at least 5 characters when validated.");
        }
        outcome.setValidated(request.validated());
        outcome.setSummary(summary);
        return repository.save(outcome);
    }

    public DialysisOutcome getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dialysis outcome not found: " + id));
    }

    public List<DialysisOutcome> getAll() {
        return repository.findAll();
    }
}
