package tn.esprit.spring.procedureservice.surgical.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgeryRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.CreateSurgeryRequest;
import tn.esprit.spring.procedureservice.surgical.dto.request.UpdateSurgeryRequestStatusRequest;
import tn.esprit.spring.procedureservice.surgical.dto.response.SurgeryRequestResponse;
import tn.esprit.spring.procedureservice.surgical.service.SurgeryRequestService;

@RestController
@RequestMapping("/api/procedures/surgical/requests")
public class SurgeryRequestController {
    private final SurgeryRequestService service;

    public SurgeryRequestController(SurgeryRequestService service) {
        this.service = service;
    }

    @PostMapping
    public SurgeryRequestResponse create(@Valid @RequestBody CreateSurgeryRequest request) {
        return toResponse(service.create(request));
    }

    @GetMapping
    public List<SurgeryRequestResponse> getAll(
        @RequestParam(required = false) String consultationId,
        @RequestParam(required = false) String status
    ) {
        return service.getAll(consultationId, status).stream()
            .map(SurgeryRequestController::toResponse)
            .toList();
    }

    @PutMapping("/{id}/status")
    public SurgeryRequestResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSurgeryRequestStatusRequest request
    ) {
        return toResponse(service.updateStatus(id, request));
    }

    private static SurgeryRequestResponse toResponse(SurgeryRequest surgeryRequest) {
        return new SurgeryRequestResponse(
            surgeryRequest.getId(),
            surgeryRequest.getPatientId(),
            surgeryRequest.getConsultationId(),
            surgeryRequest.getRequestedByDoctorId(),
            surgeryRequest.getPatientFirstName(),
            surgeryRequest.getPatientLastName(),
            surgeryRequest.getReason(),
            surgeryRequest.getUrgencyLevel(),
            surgeryRequest.getClinicalNote(),
            surgeryRequest.getStatus(),
            surgeryRequest.getCreatedAt(),
            surgeryRequest.getUpdatedAt()
        );
    }
}
