package tn.esprit.spring.opsservice.hospitalization.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationTaskRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationCaseResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationSummaryResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskUpdateRequest;
import tn.esprit.spring.opsservice.hospitalization.application.HospitalizationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HospitalizationController {

    private final HospitalizationService hospitalizationService;

    @PostMapping("/hospitalizations")
    @PreAuthorize("hasRole('DOCTOR')")
    public HospitalizationCaseResponse createHospitalization(@Valid @RequestBody CreateHospitalizationRequest request) {
        return hospitalizationService.createHospitalization(request);
    }

    @PostMapping("/hospitalizations/{hospitalizationId}/tasks")
    @PreAuthorize("hasRole('DOCTOR')")
    public HospitalizationTaskResponse addTask(
            @PathVariable UUID hospitalizationId,
            @Valid @RequestBody CreateHospitalizationTaskRequest request
    ) {
        return hospitalizationService.addTask(hospitalizationId, request);
    }

    @GetMapping("/hospitalizations/{hospitalizationId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public HospitalizationCaseResponse getHospitalization(@PathVariable UUID hospitalizationId) {
        return hospitalizationService.getHospitalization(hospitalizationId);
    }

    @GetMapping("/hospitalizations/{hospitalizationId}/progress")
    @PreAuthorize("hasRole('DOCTOR')")
    public HospitalizationCaseResponse getHospitalizationProgress(@PathVariable UUID hospitalizationId) {
        return hospitalizationService.getHospitalization(hospitalizationId);
    }

    @GetMapping("/nurse/hospitalizations/active")
    @PreAuthorize("hasRole('NURSE')")
    public List<HospitalizationSummaryResponse> getActiveHospitalizationsForNurse() {
        return hospitalizationService.getActiveHospitalizationsForNurse();
    }

    @PutMapping("/nurse/tasks/{taskId}")
    @PreAuthorize("hasRole('NURSE')")
    public HospitalizationTaskResponse updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody HospitalizationTaskUpdateRequest request
    ) {
        return hospitalizationService.recordTaskExecution(taskId, request);
    }
}
