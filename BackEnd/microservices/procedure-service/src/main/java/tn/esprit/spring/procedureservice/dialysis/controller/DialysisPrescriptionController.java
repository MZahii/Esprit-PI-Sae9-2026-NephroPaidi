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
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisPrescriptionRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.request.UpdateDialysisPrescriptionRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisPrescriptionResponse;
import tn.esprit.spring.procedureservice.dialysis.service.DialysisPrescriptionService;
import tn.esprit.spring.procedureservice.shared.mapper.DialysisMapper;

@RestController
@RequestMapping("/api/procedures/dialysis/prescriptions")
public class DialysisPrescriptionController {
    private final DialysisPrescriptionService service;

    public DialysisPrescriptionController(DialysisPrescriptionService service) {
        this.service = service;
    }

    @PostMapping
    public DialysisPrescriptionResponse create(@Valid @RequestBody CreateDialysisPrescriptionRequest request) {
        return DialysisMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public DialysisPrescriptionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDialysisPrescriptionRequest request) {
        return DialysisMapper.toResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    public DialysisPrescriptionResponse getById(@PathVariable Long id) {
        return DialysisMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public List<DialysisPrescriptionResponse> getAll() {
        return service.getAll().stream().map(DialysisMapper::toResponse).toList();
    }
}
