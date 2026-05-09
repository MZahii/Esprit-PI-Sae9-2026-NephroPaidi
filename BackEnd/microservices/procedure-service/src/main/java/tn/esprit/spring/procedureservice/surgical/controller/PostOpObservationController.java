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
import tn.esprit.spring.procedureservice.surgical.dto.request.AddObservationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateObservationRequest;
import tn.esprit.spring.procedureservice.surgical.dto.response.PostOpObservationResponse;
import tn.esprit.spring.procedureservice.surgical.service.PostOpObservationService;

@RestController
@RequestMapping("/api/procedures/surgical/postop-observations")
public class PostOpObservationController {
    private final PostOpObservationService service;

    public PostOpObservationController(PostOpObservationService service) {
        this.service = service;
    }

    @PostMapping
    public PostOpObservationResponse create(@Valid @RequestBody AddObservationRequest request) {
        return SurgicalCaseMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public PostOpObservationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateObservationRequest request) {
        return SurgicalCaseMapper.toResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    public PostOpObservationResponse getById(@PathVariable Long id) {
        return SurgicalCaseMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<PostOpObservationResponse> getAll() {
        return service.getAll().stream().map(SurgicalCaseMapper::toResponse).toList();
    }
}
