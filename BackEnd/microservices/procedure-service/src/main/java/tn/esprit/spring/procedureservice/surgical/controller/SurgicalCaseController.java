package tn.esprit.spring.procedureservice.surgical.controller;

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
import tn.esprit.spring.procedureservice.shared.mapper.SurgicalCaseMapper;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.DecideTransplantOfferRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateSurgicalCaseRequest;
import tn.esprit.spring.procedureservice.surgical.dto.response.SurgicalCaseResponse;
import tn.esprit.spring.procedureservice.surgical.service.SurgicalCasePdfService;
import tn.esprit.spring.procedureservice.surgical.service.SurgicalCaseService;

@RestController
@RequestMapping("/api/procedures/surgical/cases")
public class SurgicalCaseController {
    private final SurgicalCaseService service;
    private final SurgicalCasePdfService pdfService;

    public SurgicalCaseController(SurgicalCaseService service, SurgicalCasePdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @PostMapping
    public SurgicalCaseResponse create(@Valid @RequestBody CreateSurgicalCaseRequest request) {
        return SurgicalCaseMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    public SurgicalCaseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSurgicalCaseRequest request) {
        return SurgicalCaseMapper.toResponse(service.update(id, request));
    }

    @PutMapping("/{id}/offer")
    public SurgicalCaseResponse decideOffer(@PathVariable Long id, @Valid @RequestBody DecideTransplantOfferRequest request) {
        return SurgicalCaseMapper.toResponse(service.decideOffer(id, request));
    }

    @GetMapping("/{id}")
    public SurgicalCaseResponse getById(@PathVariable Long id) {
        return SurgicalCaseMapper.toResponse(service.getById(id));
    }

    @GetMapping("/{id}/summary-pdf")
    public ResponseEntity<byte[]> downloadSummaryPdf(@PathVariable Long id) {
        byte[] pdf = pdfService.generateCaseSummaryPdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=surgical-case-" + id + "-summary.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping
    public List<SurgicalCaseResponse> getAll() {
        return service.getAll().stream().map(SurgicalCaseMapper::toResponse).toList();
    }
}
