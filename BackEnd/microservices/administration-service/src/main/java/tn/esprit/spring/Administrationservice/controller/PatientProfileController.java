package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.CreatePatientProfileRequest;
import tn.esprit.spring.Administrationservice.dto.response.PatientProfileResponse;
import tn.esprit.spring.Administrationservice.service.PatientProfileService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @PostMapping
    public PatientProfileResponse create(@Valid @RequestBody CreatePatientProfileRequest request) {
        return patientProfileService.create(request);
    }

    @PatchMapping("/{patientId}")
    public PatientProfileResponse update(
            @PathVariable Long patientId,
            @Valid @RequestBody CreatePatientProfileRequest request
    ) {
        return patientProfileService.update(patientId, request);
    }

    @GetMapping
    public List<PatientProfileResponse> getAll() {
        return patientProfileService.getAll();
    }

    @GetMapping("/{patientId}")
    public PatientProfileResponse getById(@PathVariable Long patientId) {
        return patientProfileService.getById(patientId);
    }

    @GetMapping("/batch")
    public List<PatientProfileResponse> getByIds(@RequestParam("ids") String ids) {
        List<Long> parsedIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
        return patientProfileService.getByIds(parsedIds);
    }

    @GetMapping("/search")
    public List<PatientProfileResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return patientProfileService.search(query, limit);
    }

    @GetMapping("/guardian/{guardianUserId}")
    public List<PatientProfileResponse> getByGuardian(@PathVariable Long guardianUserId) {
        return patientProfileService.getByGuardianUserId(guardianUserId);
    }
}
