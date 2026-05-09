package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.Complication;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateComplicationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateComplicationRequest;
import tn.esprit.spring.procedureservice.surgical.repository.ComplicationRepository;

@Service
public class ComplicationService {
    private final ComplicationRepository repository;
    private final SurgicalCaseService surgicalCaseService;

    public ComplicationService(ComplicationRepository repository, SurgicalCaseService surgicalCaseService) {
        this.repository = repository;
        this.surgicalCaseService = surgicalCaseService;
    }

    public Complication create(CreateComplicationRequest request) {
        Complication complication = new Complication();
        complication.setSurgicalCase(surgicalCaseService.getById(request.surgicalCaseId()));
        complication.setDescription(request.description());
        return repository.save(complication);
    }

    public Complication update(Long id, UpdateComplicationRequest request) {
        Complication complication = getById(id);
        complication.setDescription(request.description());
        return repository.save(complication);
    }

    public Complication getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Complication not found: " + id));
    }

    public List<Complication> getAll() {
        return repository.findAll();
    }
}
