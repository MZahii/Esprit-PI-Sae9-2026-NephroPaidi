package tn.esprit.spring.procedureservice.surgical.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.shared.exception.NotFoundException;
import tn.esprit.spring.procedureservice.surgical.domain.entity.CareTask;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateCareTaskRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateCareTaskRequest;
import tn.esprit.spring.procedureservice.surgical.repository.CareTaskRepository;

@Service
public class CareTasksService {
    private final CareTaskRepository repository;
    private final SurgicalCaseService surgicalCaseService;

    public CareTasksService(CareTaskRepository repository, SurgicalCaseService surgicalCaseService) {
        this.repository = repository;
        this.surgicalCaseService = surgicalCaseService;
    }

    public CareTask create(CreateCareTaskRequest request) {
        CareTask task = new CareTask();
        task.setSurgicalCase(surgicalCaseService.getById(request.surgicalCaseId()));
        task.setTitle(request.title());
        task.setDone(false);
        return repository.save(task);
    }

    public CareTask update(Long id, UpdateCareTaskRequest request) {
        CareTask task = getById(id);
        task.setTitle(request.title());
        task.setDone(request.done());
        return repository.save(task);
    }

    public CareTask getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Care task not found: " + id));
    }

    public List<CareTask> getAll() {
        return repository.findAll();
    }
}
