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
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateComplicationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateComplicationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.response.ComplicationResponse;
import tn.esprit.spring.procedureservice.surgical.service.ComplicationService;

@RestController
@RequestMapping("/api/procedures/surgical/complications")
public class ComplicationController {
    private final ComplicationService service;

    public ComplicationController(ComplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ComplicationResponse create(@Valid @RequestBody CreateComplicationRequest request) {
        return SurgicalCaseMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public ComplicationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateComplicationRequest request) {
        return SurgicalCaseMapper.toResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ComplicationResponse getById(@PathVariable Long id) {
        return SurgicalCaseMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<ComplicationResponse> getAll() {
        return service.getAll().stream().map(SurgicalCaseMapper::toResponse).toList();
    }
}
