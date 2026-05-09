package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.CreateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.request.UpdateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.response.ActionRequiredAlertsResponse;
import tn.esprit.spring.Administrationservice.dto.response.StaffContractResponse;
import tn.esprit.spring.Administrationservice.entity.ContractStatus;
import tn.esprit.spring.Administrationservice.repository.PatientProfileRepository;
import tn.esprit.spring.Administrationservice.repository.StaffContractRepository;
import tn.esprit.spring.Administrationservice.service.StaffContractService;
import tn.esprit.spring.Administrationservice.client.UserAccessClient;

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/contracts", "/api/contracts"})
@RequiredArgsConstructor
public class StaffContractController {

    private final StaffContractService staffContractService;
    private final StaffContractRepository staffContractRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserAccessClient userAccessClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping
    public StaffContractResponse create(@Valid @RequestBody CreateStaffContractRequest request) {
        return staffContractService.create(request);
    }

    @PutMapping("/{contractId}")
    public StaffContractResponse update(
            @PathVariable Long contractId,
            @Valid @RequestBody UpdateStaffContractRequest request
    ) {
        return staffContractService.update(contractId, request);
    }

    @GetMapping("/{contractId}")
    public StaffContractResponse getById(@PathVariable Long contractId) {
        return staffContractService.getById(contractId);
    }

    @GetMapping
    public List<StaffContractResponse> getAll(
            @RequestParam(required = false) Long staffUserId,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        return staffContractService.getAll(staffUserId, status, includeDeleted);
    }

    @PatchMapping("/{contractId}/end")
    public StaffContractResponse endContract(@PathVariable Long contractId) {
        return staffContractService.endContract(contractId);
    }

    @PatchMapping("/{contractId}/suspend")
    public StaffContractResponse suspendContract(@PathVariable Long contractId) {
        return staffContractService.suspendContract(contractId);
    }

    @PatchMapping("/{contractId}/resume")
    public StaffContractResponse resumeContract(@PathVariable Long contractId) {
        return staffContractService.resumeContract(contractId);
    }

    @PatchMapping("/staff/{staffUserId}/access")
    public Map<String, Object> updateStaffAccess(
            @PathVariable Long staffUserId,
            @RequestParam boolean enabled
    ) {
        staffContractService.updateStaffAccess(staffUserId, enabled);
        return Map.of(
                "staffUserId", staffUserId,
                "enabled", enabled,
                "message", enabled ? "Staff access enabled." : "Staff access disabled."
        );
    }

    @PatchMapping("/staff/{staffUserId}/status")
    public Map<String, Object> updateStaffStatus(
            @PathVariable Long staffUserId,
            @RequestParam String status
    ) {
        staffContractService.updateStaffStatus(staffUserId, status);
        return Map.of(
                "staffUserId", staffUserId,
                "status", status,
                "message", "Staff account status updated."
        );
    }

    @DeleteMapping("/{contractId}")
    public void delete(@PathVariable Long contractId) {
        staffContractService.delete(contractId);
    }

    @PatchMapping("/{contractId}/restore")
    public StaffContractResponse restore(@PathVariable Long contractId) {
        return staffContractService.restore(contractId);
    }

    @GetMapping("/alerts/action-required")
    public ActionRequiredAlertsResponse getActionRequiredAlerts(@RequestParam(defaultValue = "7") int pendingDays) {
        LocalDate now = LocalDate.now();
        long endingIn7 = staffContractRepository.findByStatusInAndEndDateBetweenAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED),
                now,
                now.plusDays(7)
        ).size();

        long endingIn30 = staffContractRepository.findByStatusInAndEndDateBetweenAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED),
                now,
                now.plusDays(30)
        ).size();

        long pendingTooLong = userAccessClient.getPendingContractCountOlderThanDays(pendingDays, internalApiKey);
        long missingProfiles = patientProfileRepository.countProfilesMissingRequiredData();

        return ActionRequiredAlertsResponse.builder()
                .contractsEndingIn7Days(endingIn7)
                .contractsEndingIn30Days(endingIn30)
                .pendingUsersTooLong(pendingTooLong)
                .profilesMissingRequiredData(missingProfiles)
                .build();
    }

    @PatchMapping("/maintenance/expire")
    public int expireContractsNow() {
        return staffContractService.expireContractsAndDisableAccess();
    }
}
