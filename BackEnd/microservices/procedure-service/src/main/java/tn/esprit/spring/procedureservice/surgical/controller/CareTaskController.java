package tn.esprit.spring.procedureservice.surgical.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.shared.mapper.SurgicalCaseMapper;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateCareTaskRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateCareTaskRequest;
import tn.esprit.spring.procedureservice.surgical.dto.response.CareTaskResponse;
import tn.esprit.spring.procedureservice.surgical.service.CareTasksService;

@RestController
@RequestMapping("/api/procedures/surgical/care-tasks")
public class CareTaskController {
    private final CareTasksService service;

    public CareTaskController(CareTasksService service) {
        this.service = service;
    }

    @PostMapping
    public CareTaskResponse create(@Valid @RequestBody CreateCareTaskRequest request) {
        return SurgicalCaseMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public CareTaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCareTaskRequest request) {
        return SurgicalCaseMapper.toResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    public CareTaskResponse getById(@PathVariable Long id) {
        return SurgicalCaseMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<CareTaskResponse> getAll() {
        return service.getAll().stream().map(SurgicalCaseMapper::toResponse).toList();
    }
}
