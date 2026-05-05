package tn.esprit.spring.Administrationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.spring.Administrationservice.client.UserAccessClient;
import tn.esprit.spring.Administrationservice.client.InternalUserSummary;
import tn.esprit.spring.Administrationservice.dto.request.CreateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.request.UpdateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.response.StaffContractResponse;
import tn.esprit.spring.Administrationservice.entity.ContractStatus;
import tn.esprit.spring.Administrationservice.entity.StaffContract;
import tn.esprit.spring.Administrationservice.repository.StaffContractRepository;
import tn.esprit.spring.Administrationservice.service.StaffContractService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffContractServiceImpl implements StaffContractService {

    private final StaffContractRepository staffContractRepository;
    private final UserAccessClient userAccessClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private static final Set<String> CONTRACT_ELIGIBLE_ROLES = Set.of(
            "DOCTOR",
            "NURSE",
            "SURGEON",
            "PHARMACIST",
            "RECEPTIONIST",
            "LAB_AGENT",
            "HR"
    );

    @Override
    @Transactional
    public StaffContractResponse create(CreateStaffContractRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        InternalUserSummary userSummary = requireEligibleUserForContract(request.getStaffUserId());

        boolean alreadyHasRunningContract = staffContractRepository.existsByStaffUserIdAndStatusInAndDeletedFalse(
                request.getStaffUserId(),
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED)
        );

        if (alreadyHasRunningContract) {
            throw new IllegalArgumentException("This staff account already has a running contract.");
        }

        String generatedReference = generateContractReference(request.getStaffUserId(), userSummary.getRole());

        StaffContract contract = staffContractRepository.save(
                StaffContract.builder()
                        .staffUserId(request.getStaffUserId())
                        .contractReference(generatedReference)
                        .contractType(request.getContractType())
                        .status(ContractStatus.ACTIVE)
                        .jobTitle(request.getJobTitle().trim())
                        .department(trimOrNull(request.getDepartment()))
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .salary(request.getSalary())
                        .currency(trimOrNull(request.getCurrency()))
                        .hoursPerWeek(request.getHoursPerWeek())
                        .notes(trimOrNull(request.getNotes()))
                        .build()
        );

        setUserAccess(contract.getStaffUserId(), true);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(contract);
    }

    @Override
    @Transactional
    public StaffContractResponse update(Long contractId, UpdateStaffContractRequest request) {
        StaffContract contract = getContractOrThrow(contractId);
        Map<String, Object> before = contractSnapshot(contract);
        validateDates(request.getStartDate(), request.getEndDate());

        contract.setContractType(request.getContractType());
        contract.setJobTitle(request.getJobTitle().trim());
        contract.setDepartment(trimOrNull(request.getDepartment()));
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setSalary(request.getSalary());
        contract.setCurrency(trimOrNull(request.getCurrency()));
        contract.setHoursPerWeek(request.getHoursPerWeek());
        contract.setNotes(trimOrNull(request.getNotes()));

        StaffContract saved = staffContractRepository.save(contract);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(saved);
    }

    @Override
    public StaffContractResponse getById(Long contractId) {
        return StaffContractResponse.from(getContractOrThrow(contractId));
    }

    @Override
    public List<StaffContractResponse> getAll(Long staffUserId, ContractStatus status, boolean includeDeleted) {
        List<StaffContract> contracts;
        boolean allowDeleted = includeDeleted && isActorAdmin();

        if (allowDeleted) {
            contracts = staffContractRepository.findAll();
            if (staffUserId != null) {
                contracts = contracts.stream()
                        .filter(contract -> contract.getStaffUserId().equals(staffUserId))
                        .toList();
            }
            if (status != null) {
                contracts = contracts.stream()
                        .filter(contract -> contract.getStatus() == status)
                        .toList();
            }
        } else {
            if (staffUserId != null && status != null) {
                contracts = staffContractRepository.findByStaffUserIdAndDeletedFalse(staffUserId)
                        .stream()
                        .filter(contract -> contract.getStatus() == status)
                        .toList();
            } else if (staffUserId != null) {
                contracts = staffContractRepository.findByStaffUserIdAndDeletedFalse(staffUserId);
            } else if (status != null) {
                contracts = staffContractRepository.findByStatusAndDeletedFalse(status);
            } else {
                contracts = staffContractRepository.findAll().stream().filter(c -> !c.isDeleted()).toList();
            }
        }

        return contracts.stream()
                .map(StaffContractResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public StaffContractResponse endContract(Long contractId) {
        StaffContract contract = getContractOrThrow(contractId);
        Map<String, Object> before = contractSnapshot(contract);
        contract.setStatus(ContractStatus.ENDED);
        if (contract.getEndDate().isAfter(LocalDate.now())) {
            contract.setEndDate(LocalDate.now());
        }
        StaffContract saved = staffContractRepository.save(contract);
        setUserAccess(contract.getStaffUserId(), false);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(saved);
    }

    @Override
    @Transactional
    public StaffContractResponse suspendContract(Long contractId) {
        StaffContract contract = getContractOrThrow(contractId);
        Map<String, Object> before = contractSnapshot(contract);
        if (contract.getStatus() == ContractStatus.ENDED || contract.getStatus() == ContractStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot suspend ended/expired contract.");
        }
        contract.setStatus(ContractStatus.SUSPENDED);
        StaffContract saved = staffContractRepository.save(contract);
        setUserAccess(contract.getStaffUserId(), false);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(saved);
    }

    @Override
    @Transactional
    public StaffContractResponse resumeContract(Long contractId) {
        StaffContract contract = getContractOrThrow(contractId);
        Map<String, Object> before = contractSnapshot(contract);
        if (contract.getStatus() == ContractStatus.ENDED || contract.getStatus() == ContractStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot resume ended/expired contract.");
        }
        if (contract.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot resume contract because it is already past end date.");
        }
        contract.setStatus(ContractStatus.ACTIVE);
        StaffContract saved = staffContractRepository.save(contract);
        setUserAccess(contract.getStaffUserId(), true);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(saved);
    }

    @Override
    public void updateStaffAccess(Long staffUserId, boolean enabled) {
        setUserAccess(staffUserId, enabled);
    }

    @Override
    public void updateStaffStatus(Long staffUserId, String status) {
        try {
            userAccessClient.updateStatus(staffUserId, status, internalApiKey, resolveActorForInternalCalls());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to sync account status with user-service. Cause: " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    @Transactional
    public void delete(Long contractId) {
        StaffContract contract = getContractOrThrow(contractId);
        validateArchivePermission(contract);
        Map<String, Object> before = contractSnapshot(contract);
        if (contract.getStatus() == ContractStatus.ACTIVE || contract.getStatus() == ContractStatus.SUSPENDED) {
            setUserAccess(contract.getStaffUserId(), false);
        }
        contract.setDeleted(true);
        contract.setDeletedAt(LocalDateTime.now());
        contract.setDeletedBy(resolveActorForInternalCalls());
        staffContractRepository.save(contract);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
    }

    @Override
    @Transactional
    public StaffContractResponse restore(Long contractId) {
        if (!isActorAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can restore archived contracts.");
        }
        StaffContract contract = staffContractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + contractId));
        if (!contract.isDeleted()) {
            return StaffContractResponse.from(contract);
        }

        contract.setDeleted(false);
        contract.setDeletedAt(null);
        contract.setDeletedBy(null);
        StaffContract saved = staffContractRepository.save(contract);
        // TODO: Add audit logging and notifications when ObservabilityService is implemented
        return StaffContractResponse.from(saved);
    }

    @Override
    @Transactional
    public int expireContractsAndDisableAccess() {
        List<StaffContract> toExpire = staffContractRepository.findByStatusInAndEndDateBeforeAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED),
                LocalDate.now()
        );

        for (StaffContract contract : toExpire) {
            contract.setStatus(ContractStatus.EXPIRED);
            setUserAccess(contract.getStaffUserId(), false);
            // TODO: Implement audit event logging and push notifications when ObservabilityService is ready
        }
        return toExpire.size();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledExpirationCheck() {
        int expiredCount = expireContractsAndDisableAccess();
        if (expiredCount > 0) {
            log.info("Expired {} contracts and disabled linked accounts.", expiredCount);
        }

        LocalDate today = LocalDate.now();
        LocalDate inSevenDays = today.plusDays(7);
        List<StaffContract> upcoming = staffContractRepository.findByStatusInAndEndDateBetweenAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED),
                today,
                inSevenDays
        );
        for (StaffContract contract : upcoming) {
            // TODO: Implement audit event logging and push notifications when ObservabilityService is ready
        }
    }

    private StaffContract getContractOrThrow(Long contractId) {
        return staffContractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + contractId));
    }

    private void setUserAccess(Long staffUserId, boolean enabled) {
        try {
            userAccessClient.updateActivation(staffUserId, enabled, internalApiKey, resolveActorForInternalCalls());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Contract updated but failed to sync user access with user-service. Cause: " + ex.getMessage(),
                    ex
            );
        }
    }

    private String resolveActorForInternalCalls() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                String actor = attributes.getRequest().getHeader("X-Actor-Username");
                if (actor != null && !actor.isBlank()) {
                    return actor.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "SYSTEM";
    }

    private String resolveActorRole() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                String role = attributes.getRequest().getHeader("X-Actor-Role");
                if (role != null && !role.isBlank()) {
                    String normalized = role.trim().toUpperCase();
                    return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean isActorAdmin() {
        return "ADMIN".equals(resolveActorRole());
    }

    private void validateArchivePermission(StaffContract contract) {
        String actorRole = resolveActorRole();
        InternalUserSummary ownerSummary = userAccessClient.getUserSummary(contract.getStaffUserId(), internalApiKey);
        String ownerRole = ownerSummary != null && ownerSummary.getRole() != null
                ? ownerSummary.getRole().trim().toUpperCase()
                : "";

        boolean adminArchivingHr = "ADMIN".equals(actorRole) && "HR".equals(ownerRole);
        boolean hrArchivingStaff = "HR".equals(actorRole) && !"HR".equals(ownerRole);
        if (!(adminArchivingHr || hrArchivingStaff)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Archive not allowed for this contract and role combination."
            );
        }
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date.");
        }
    }

    private String generateContractReference(Long staffUserId) {
        return generateContractReference(staffUserId, null);
    }

    private String generateContractReference(Long staffUserId, String roleFromCaller) {
        String role = roleFromCaller;
        if (role == null || role.isBlank()) {
            InternalUserSummary userSummary = userAccessClient.getUserSummary(staffUserId, internalApiKey);
            role = userSummary != null && userSummary.getRole() != null ? userSummary.getRole() : "";
        }
        String prefix = switch (role) {
            case "DOCTOR" -> "doc";
            case "NURSE" -> "nurse";
            case "SURGEON" -> "surg";
            case "PHARMACIST" -> "pharm";
            case "RECEPTIONIST" -> "recp";
            case "LAB_AGENT" -> "lab";
            case "HR" -> "hr";
            default -> "staff";
        };

        String candidate;
        int attempts = 0;
        do {
            long suffix = (System.currentTimeMillis() + (staffUserId * 97) + attempts) % 100000;
            candidate = prefix + String.format("%05d", suffix);
            attempts++;
            if (attempts > 20) {
                candidate = prefix + staffUserId + String.format("%03d", attempts);
                break;
            }
        } while (staffContractRepository.findByContractReferenceAndDeletedFalse(candidate).isPresent());

        return candidate;
    }

    private Map<String, Object> contractSnapshot(StaffContract contract) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", contract.getId());
        snapshot.put("staffUserId", contract.getStaffUserId());
        snapshot.put("reference", contract.getContractReference());
        snapshot.put("type", String.valueOf(contract.getContractType()));
        snapshot.put("status", String.valueOf(contract.getStatus()));
        snapshot.put("jobTitle", String.valueOf(contract.getJobTitle()));
        snapshot.put("department", String.valueOf(contract.getDepartment()));
        snapshot.put("startDate", String.valueOf(contract.getStartDate()));
        snapshot.put("endDate", String.valueOf(contract.getEndDate()));
        snapshot.put("salary", String.valueOf(contract.getSalary()));
        snapshot.put("hoursPerWeek", String.valueOf(contract.getHoursPerWeek()));
        snapshot.put("deleted", contract.isDeleted());
        return snapshot;
    }

    private InternalUserSummary requireEligibleUserForContract(Long userId) {
        InternalUserSummary userSummary = userAccessClient.getUserSummary(userId, internalApiKey);
        if (userSummary == null || userSummary.getRole() == null) {
            throw new IllegalArgumentException("User not found or missing role for id: " + userId);
        }

        String role = userSummary.getRole().trim().toUpperCase();
        if (!CONTRACT_ELIGIBLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Contracts are not allowed for role: " + role);
        }

        return userSummary;
    }

    private String buildContractChangesMessage(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return "No detailed field diff available.";
        }
        List<String> changes = new java.util.ArrayList<>();
        for (String key : after.keySet()) {
            if ("id".equals(key) || "staffUserId".equals(key)) {
                continue;
            }
            Object oldVal = before.get(key);
            Object newVal = after.get(key);
            if (!java.util.Objects.equals(oldVal, newVal)) {
                changes.add(humanizeContractField(key) + ": " + stringifyValue(oldVal) + " -> " + stringifyValue(newVal));
            }
        }
        if (changes.isEmpty()) {
            return "No visible field changes.";
        }
        return "Changed: " + String.join(" | ", changes);
    }

    private String humanizeContractField(String key) {
        return switch (key) {
            case "jobTitle" -> "Job Title";
            case "contractType", "type" -> "Contract Type";
            case "contractReference", "reference" -> "Reference";
            case "startDate" -> "Start Date";
            case "endDate" -> "End Date";
            case "hoursPerWeek" -> "Hours / Week";
            default -> key.replaceAll("([a-z])([A-Z])", "$1 $2");
        };
    }

    private String stringifyValue(Object value) {
        if (value == null) return "-";
        String raw = String.valueOf(value);
        return raw.isBlank() ? "-" : raw;
    }
}
