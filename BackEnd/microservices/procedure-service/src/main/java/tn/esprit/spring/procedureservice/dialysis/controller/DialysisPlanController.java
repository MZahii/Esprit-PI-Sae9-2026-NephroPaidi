package tn.esprit.spring.procedureservice.dialysis.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.dialysis.dto.request.CreateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.request.UpdateDialysisPlanRequest;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisPlanResponse;
import tn.esprit.spring.procedureservice.dialysis.service.DialysisPlanPdfService;
import tn.esprit.spring.procedureservice.dialysis.service.DialysisPlanService;
import tn.esprit.spring.procedureservice.shared.mapper.DialysisMapper;

@RestController
@RequestMapping("/api/procedures/dialysis/plans")
public class DialysisPlanController {
    private final DialysisPlanService service;
    private final DialysisPlanPdfService pdfService;

    public DialysisPlanController(DialysisPlanService service, DialysisPlanPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @PostMapping
    public DialysisPlanResponse create(@Valid @RequestBody CreateDialysisPlanRequest request) {
        return DialysisMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public DialysisPlanResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDialysisPlanRequest request) {
        return DialysisMapper.toResponse(service.update(id, request));
    }

    @GetMapping("/{id}")
    public DialysisPlanResponse getById(@PathVariable Long id) {
        return DialysisMapper.toResponse(service.getById(id));
    }

    @GetMapping("/{id}/summary-pdf")
    public ResponseEntity<byte[]> downloadSummaryPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generatePlanSummaryPdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dialysis-plan-" + id + "-summary.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping
    public List<DialysisPlanResponse> getAll() {
        return service.getAll().stream().map(DialysisMapper::toResponse).toList();
    }
}
