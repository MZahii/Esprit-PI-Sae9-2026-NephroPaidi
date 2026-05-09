package tn.esprit.spring.procedureservice.dialysis.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisSessionRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisSessionResponse;
import tn.esprit.spring.procedureservice.dialysis.service.DialysisSessionService;
import tn.esprit.spring.procedureservice.shared.mapper.DialysisMapper;

@RestController
@RequestMapping("/api/procedures/dialysis/sessions")
public class DialysisSessionController {
    private final DialysisSessionService service;

    public DialysisSessionController(DialysisSessionService service) {
        this.service = service;
    }

    @PostMapping
    public DialysisSessionResponse create(@Valid @RequestBody CreateDialysisSessionRequest request) {
        return DialysisMapper.toResponse(service.create(request));
    }

    @PostMapping("/generate/plan/{planId}")
    public List<DialysisSessionResponse> generateFromPlan(@PathVariable Long planId) {
        return service.generateFromPlan(planId).stream().map(DialysisMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DialysisSessionResponse getById(@PathVariable Long id) {
        return DialysisMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<DialysisSessionResponse> getAll() {
        return service.getAll().stream().map(DialysisMapper::toResponse).toList();
    }
}
