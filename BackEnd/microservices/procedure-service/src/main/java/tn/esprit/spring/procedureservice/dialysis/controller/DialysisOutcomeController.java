package tn.esprit.spring.procedureservice.dialysis.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.dialysis.dto.request.ValidateDialysisOutcomeRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisOutcomeResponse;
import tn.esprit.spring.procedureservice.dialysis.service.DialysisOutcomeService;
import tn.esprit.spring.procedureservice.shared.mapper.DialysisMapper;

@RestController
@RequestMapping("/api/procedures/dialysis/outcomes")
public class DialysisOutcomeController {
    private final DialysisOutcomeService service;

    public DialysisOutcomeController(DialysisOutcomeService service) {
        this.service = service;
    }

    @PostMapping("/session/{sessionId}")
    public DialysisOutcomeResponse createForSession(@PathVariable Long sessionId) {
        return DialysisMapper.toResponse(service.createForSession(sessionId));
    }

    @PutMapping("/{id}/validate")
    public DialysisOutcomeResponse validate(@PathVariable Long id, @Valid @RequestBody ValidateDialysisOutcomeRequest request) {
        return DialysisMapper.toResponse(service.validate(id, request));
    }

    @GetMapping("/{id}")
    public DialysisOutcomeResponse getById(@PathVariable Long id) {
        return DialysisMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<DialysisOutcomeResponse> getAll() {
        return service.getAll().stream().map(DialysisMapper::toResponse).toList();
    }
}
