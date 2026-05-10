package tn.esprit.spring.userservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.spring.userservice.dto.request.CreateGuardianAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateHrAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateInternalUserRequest;
import tn.esprit.spring.userservice.dto.request.CreateStaffAccountRequest;
import tn.esprit.spring.userservice.dto.request.StaffSearchRequest;
import tn.esprit.spring.userservice.dto.request.UpdateGuardianProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateHrProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateStaffProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyPreferencesRequest;
import tn.esprit.spring.userservice.dto.request.ChangeMyPasswordRequest;
import tn.esprit.spring.userservice.dto.response.MyAccountSettingsResponse;
import tn.esprit.spring.userservice.dto.response.StaffSearchResponse;
import tn.esprit.spring.userservice.dto.response.UserAuditLogResponse;
import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.User;
import tn.esprit.spring.userservice.entity.UserAuditLog;
import tn.esprit.spring.userservice.mapper.UserMapper;
import tn.esprit.spring.userservice.repository.UserAuditLogRepository;
import tn.esprit.spring.userservice.service.InternalNotificationBridgeService;
import tn.esprit.spring.userservice.repository.UserRepository;
import tn.esprit.spring.userservice.service.KeycloakAdminService;
import tn.esprit.spring.userservice.service.UserService;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final InternalNotificationBridgeService notificationBridgeService;
    private final UserAuditLogRepository userAuditLogRepository;
    private final ObjectMapper objectMapper;

    private static final Set<Role> ALLOWED_STAFF_ROLES = Set.of(
            Role.DOCTOR,
            Role.NURSE,
            Role.SURGEON,
            Role.PHARMACIST,
            Role.RECEPTIONIST,
            Role.LAB_AGENT
    );

    @Override
    public UserResponse createHr(CreateHrAccountRequest request) {
        validateHrRequest(request);

        String generatedPassword = request.getCin();

        String keycloakId = keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                generatedPassword,
                request.getCin(),
                Role.HR.name(),
                false
        );

        User user = persistUserWithRollback(
                User.builder()
                        .keycloakId(keycloakId)
                        .username(request.getUsername())
                        .cin(request.getCin())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .sex(request.getSex())
                        .role(Role.HR)
                        .accountStatus(AccountStatus.PENDING_CONTRACT)
                        .enabled(false)
                        .mustChangePassword(true)
                        .avatarUrl(trimOrNull(request.getAvatarUrl()))
                        .build(),
                keycloakId,
                "HR"
        );
        triggerEmailVerificationWorkflow(user);
        saveAudit(user.getId(), "HrAccountCreated", null, userSnapshot(user));

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse createInternalUser(CreateInternalUserRequest request) {
        if (request.getRole() == null || !ALLOWED_STAFF_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("HR cannot create role: " + request.getRole());
        }

        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (request.getEmail() != null && userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String generatedPassword = "Temp123!";

        String keycloakId = keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getUsername(),
                request.getRole().name(),
                generatedPassword,
                null,
                request.getRole().name(),
                false
        );

        User user = persistUserWithRollback(
                User.builder()
                        .keycloakId(keycloakId)
                        .username(request.getUsername())
                        .cin("TEMP-" + System.currentTimeMillis())
                        .firstName(request.getUsername())
                        .lastName(request.getRole().name())
                        .email(request.getEmail())
                        .role(request.getRole())
                        .accountStatus(AccountStatus.PENDING_CONTRACT)
                        .enabled(false)
                        .mustChangePassword(true)
                        .build(),
                keycloakId,
                "internal staff"
        );
        triggerEmailVerificationWorkflow(user);
        saveAudit(user.getId(), "InternalAccountCreated", null, userSnapshot(user));

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse createStaff(CreateStaffAccountRequest request) {
        validateStaffRequest(request);

        if (request.getRole() == null || !ALLOWED_STAFF_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Invalid staff role: " + request.getRole());
        }

        String generatedPassword = request.getCin();

        String keycloakId = keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                generatedPassword,
                request.getCin(),
                request.getRole().name(),
                false
        );

        User user = persistUserWithRollback(
                User.builder()
                        .keycloakId(keycloakId)
                        .username(request.getUsername())
                        .cin(request.getCin())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .sex(request.getSex())
                        .role(request.getRole())
                        .accountStatus(AccountStatus.PENDING_CONTRACT)
                        .enabled(false)
                        .mustChangePassword(true)
                        .avatarUrl(trimOrNull(request.getAvatarUrl()))
                        .build(),
                keycloakId,
                "staff"
        );
        triggerEmailVerificationWorkflow(user);
        saveAudit(user.getId(), "StaffAccountCreated", null, userSnapshot(user));

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse createGuardian(CreateGuardianAccountRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (request.getEmail() != null && userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByCinAndDeletedFalse(request.getCin())) {
            throw new IllegalArgumentException("CIN already exists");
        }

        if (request.getPhone() != null && userRepository.existsByPhoneAndDeletedFalse(request.getPhone())) {
            throw new IllegalArgumentException("Phone already exists");
        }

        String generatedPassword = request.getCin();

        String keycloakId = keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                generatedPassword,
                request.getCin(),
                Role.GUARDIAN.name(),
                true
        );

        User user = persistUserWithRollback(
                User.builder()
                        .keycloakId(keycloakId)
                        .username(request.getUsername())
                        .cin(request.getCin())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .sex(request.getSex())
                        .role(Role.GUARDIAN)
                        .accountStatus(AccountStatus.ACTIVE)
                        .enabled(true)
                        .mustChangePassword(false)
                        .build(),
                keycloakId,
                "guardian"
        );
        triggerEmailVerificationWorkflow(user);
        saveAudit(user.getId(), "GuardianAccountCreated", null, userSnapshot(user));

        return UserMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getGuardians() {
        return userRepository.findByRoleAndDeletedFalse(Role.GUARDIAN)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getDoctors() {
        return userRepository.findByRoleAndDeletedFalse(Role.DOCTOR)
                .stream()
                .filter(User::isEnabled)
                .sorted(Comparator
                        .comparing((User user) -> user.getFirstName() == null ? "" : user.getFirstName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(user -> user.getLastName() == null ? "" : user.getLastName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(user -> user.getUsername() == null ? "" : user.getUsername(), String.CASE_INSENSITIVE_ORDER))
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateActivation(Long userId, boolean enabled) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        keycloakAdminService.updateUserEnabled(user.getKeycloakId(), enabled);

        user.setEnabled(enabled);
        user.setAccountStatus(enabled ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);

        User saved = userRepository.save(user);
        if (enabled) {
            triggerEmailVerificationWorkflow(saved);
        }
        saveAudit(saved.getId(), "AccountStatusChanged", before, userSnapshot(saved));
        notifyAccountStatusChanged(saved, before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateAccountStatus(Long userId, AccountStatus accountStatus) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        boolean enabled = accountStatus == AccountStatus.ACTIVE;
        keycloakAdminService.updateUserEnabled(user.getKeycloakId(), enabled);
        user.setAccountStatus(accountStatus);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        if (enabled) {
            triggerEmailVerificationWorkflow(saved);
        }
        saveAudit(saved.getId(), "AccountStatusChanged", before, userSnapshot(saved));
        notifyAccountStatusChanged(saved, before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateStaffProfile(Long userId, UpdateStaffProfileRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        if (!ALLOWED_STAFF_ROLES.contains(user.getRole())) {
            throw new IllegalArgumentException("This endpoint is allowed only for staff accounts.");
        }

        if (request.getRole() == null || !ALLOWED_STAFF_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Invalid staff role: " + request.getRole());
        }

        String normalizedEmail = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String normalizedPhone = request.getPhone() == null ? null : request.getPhone().trim();
        if (normalizedPhone != null && !normalizedPhone.isEmpty() && userRepository.existsByPhoneAndIdNot(normalizedPhone, userId)) {
            throw new IllegalArgumentException("Phone already exists");
        }

        keycloakAdminService.updateUserProfileAndRole(
                user.getKeycloakId(),
                normalizedEmail,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getRole()
        );
        triggerEmailVerificationWorkflowByChange(user, normalizedEmail);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone((normalizedPhone == null || normalizedPhone.isEmpty()) ? null : normalizedPhone);
        user.setDateOfBirth(request.getDateOfBirth());
        user.setSex(request.getSex());
        user.setRole(request.getRole());
        user.setAvatarUrl(trimOrNull(request.getAvatarUrl()));

        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "StaffProfileUpdated", before, userSnapshot(saved));
        notifyProfileUpdated(saved, before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateHrProfile(Long userId, UpdateHrProfileRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        if (user.getRole() != Role.HR) {
            throw new IllegalArgumentException("This endpoint is allowed only for HR accounts.");
        }

        String normalizedEmail = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new IllegalArgumentException("Email already exists");
        }

        keycloakAdminService.updateUserProfileAndRole(
                user.getKeycloakId(),
                normalizedEmail,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                Role.HR
        );
        triggerEmailVerificationWorkflowByChange(user, normalizedEmail);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setDateOfBirth(request.getDateOfBirth());
        user.setSex(request.getSex());
        user.setRole(Role.HR);

        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "HrProfileUpdated", before, userSnapshot(saved));
        notifyProfileUpdated(saved, before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateGuardianProfile(Long userId, UpdateGuardianProfileRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        if (user.getRole() != Role.GUARDIAN) {
            throw new IllegalArgumentException("This endpoint is allowed only for guardian accounts.");
        }

        String normalizedEmail = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String normalizedPhone = request.getPhone() == null ? null : request.getPhone().trim();
        if (normalizedPhone != null && !normalizedPhone.isEmpty() && userRepository.existsByPhoneAndIdNot(normalizedPhone, userId)) {
            throw new IllegalArgumentException("Phone already exists");
        }

        keycloakAdminService.updateUserProfileAndRole(
                user.getKeycloakId(),
                normalizedEmail,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                Role.GUARDIAN
        );
        triggerEmailVerificationWorkflowByChange(user, normalizedEmail);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone((normalizedPhone == null || normalizedPhone.isEmpty()) ? null : normalizedPhone);

        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "GuardianProfileUpdated", before, userSnapshot(saved));
        notifyProfileUpdated(saved, before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return UserMapper.toResponse(user);
    }

    @Override
    public List<UserAuditLogResponse> getUserAuditLogs(Long userId) {
        return userAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(UserAuditLogResponse::from)
                .toList();
    }

    @Override
    public List<UserAuditLogResponse> getAllUserAuditLogs() {
        return userAuditLogRepository.findTop500ByOrderByCreatedAtDesc()
                .stream()
                .map(UserAuditLogResponse::from)
                .toList();
    }

    @Override
    public StaffSearchResponse searchStaff(StaffSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(100, Math.max(1, request.getSize()));
        String sortBy = (request.getSortBy() == null || request.getSortBy().isBlank()) ? "firstName" : request.getSortBy();
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<User> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        spec = spec.and((root, query, cb) -> root.get("role").in(ALLOWED_STAFF_ROLES));

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("role").in(request.getRoles()));
        }
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("accountStatus").in(request.getStatuses()));
        }
        if (request.getEnabled() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), request.getEnabled()));
        }
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            String like = "%" + request.getQuery().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("phone")), like)
            ));
        }

        var resultPage = userRepository.findAll(spec, pageable);
        return StaffSearchResponse.builder()
                .items(resultPage.getContent().stream().map(UserMapper::toResponse).toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .build();
    }

    @Override
    public long countPendingUsersOlderThanDays(int days) {
        int safeDays = Math.max(1, days);
        return userRepository.countPendingContractOlderThan(LocalDateTime.now().minusDays(safeDays));
    }

    @Override
    public UserResponse softDeleteUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Map<String, Object> before = userSnapshot(user);

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);
        user.setAccountStatus(AccountStatus.INACTIVE);
        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "UserSoftDeleted", before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (!user.isDeleted()) {
            return UserMapper.toResponse(user);
        }
        Map<String, Object> before = userSnapshot(user);

        user.setDeleted(false);
        user.setDeletedAt(null);
        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "UserRestored", before, userSnapshot(saved));
        return UserMapper.toResponse(saved);
    }

    @Override
    public MyAccountSettingsResponse getMySettings(String authorization) {
        User user = resolveAuthenticatedUserFromAuthorization(authorization);
        return MyAccountSettingsResponse.from(user);
    }

    @Override
    public MyAccountSettingsResponse updateMyProfile(String authorization, UpdateMyProfileRequest request) {
        User user = resolveAuthenticatedUserFromAuthorization(authorization);
        Map<String, Object> before = userSnapshot(user);

        String normalizedEmail = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String normalizedPhone = request.getPhone() == null ? null : request.getPhone().trim();
        if (normalizedPhone != null && !normalizedPhone.isEmpty() && userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new IllegalArgumentException("Phone already exists");
        }

        keycloakAdminService.updateUserProfile(
                user.getKeycloakId(),
                normalizedEmail,
                request.getFirstName().trim(),
                request.getLastName().trim()
        );
        triggerEmailVerificationWorkflowByChange(user, normalizedEmail);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone((normalizedPhone == null || normalizedPhone.isEmpty()) ? null : normalizedPhone);
        user.setAvatarUrl(trimOrNull(request.getAvatarUrl()));

        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "MyProfileUpdated", before, userSnapshot(saved));
        notifyProfileUpdated(saved, before, userSnapshot(saved));
        return MyAccountSettingsResponse.from(saved);
    }

    @Override
    public MyAccountSettingsResponse updateMyPreferences(String authorization, UpdateMyPreferencesRequest request) {
        User user = resolveAuthenticatedUserFromAuthorization(authorization);
        Map<String, Object> before = userSnapshot(user);

        String preferredLanguage = normalizeLanguage(request.getPreferredLanguage());
        String theme = normalizeTheme(request.getTheme());

        user.setPreferredLanguage(preferredLanguage);
        user.setTheme(theme);
        user.setNotificationsEnabled(request.getNotificationsEnabled() == null || request.getNotificationsEnabled());

        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "MyPreferencesUpdated", before, userSnapshot(saved));
        return MyAccountSettingsResponse.from(saved);
    }

    @Override
    public void changeMyPassword(String authorization, ChangeMyPasswordRequest request) {
        User user = resolveAuthenticatedUserFromAuthorization(authorization);
        Map<String, Object> before = userSnapshot(user);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password.");
        }
        if (request.getNewPassword().contains(" ")) {
            throw new IllegalArgumentException("New password cannot contain spaces.");
        }

        boolean validCurrentPassword = keycloakAdminService.validateCredentials(user.getUsername(), request.getCurrentPassword());
        if (!validCurrentPassword) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        keycloakAdminService.updatePassword(user.getKeycloakId(), request.getNewPassword(), false);
        user.setMustChangePassword(false);
        User saved = userRepository.save(user);
        saveAudit(saved.getId(), "PasswordChanged", before, userSnapshot(saved));
        notificationBridgeService.pushNotification(
                "PasswordChanged",
                "Password Updated",
                "Your account password was changed successfully.",
                saved.getId()
        );
    }

    private void validateHrRequest(CreateHrAccountRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByCinAndDeletedFalse(request.getCin())) {
            throw new IllegalArgumentException("CIN already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhoneAndDeletedFalse(request.getPhone())) {
            throw new IllegalArgumentException("Phone already exists");
        }
    }

    private void validateStaffRequest(CreateStaffAccountRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByCinAndDeletedFalse(request.getCin())) {
            throw new IllegalArgumentException("CIN already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhoneAndDeletedFalse(request.getPhone())) {
            throw new IllegalArgumentException("Phone already exists");
        }
    }

    private User resolveAuthenticatedUserFromAuthorization(String authorization) {
        String username = extractUsernameFromBearer(authorization);
        String keycloakId = extractSubjectFromBearer(authorization);

        if (username != null && !username.isBlank()) {
            return userRepository.findByUsernameAndDeletedFalse(username)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        }

        if (keycloakId != null && !keycloakId.isBlank()) {
            return userRepository.findByKeycloakIdAndDeletedFalse(keycloakId)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        }

        throw new IllegalArgumentException("Authorization token is missing or invalid.");
    }

    private User persistUserWithRollback(User user, String keycloakId, String accountTypeLabel) {
        try {
            return userRepository.save(user);
        } catch (Exception ex) {
            keycloakAdminService.deleteUser(keycloakId);
            throw new IllegalArgumentException(
                    "Failed to save " + accountTypeLabel + " account in database: " + rootCauseMessage(ex)
            );
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getMessage();
        }
        return message;
    }

    private void saveAudit(Long userId, String action, Object oldValue, Object newValue) {
        userAuditLogRepository.save(
                UserAuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .actor(resolveActor())
                        .oldValue(writeJson(oldValue))
                        .newValue(writeJson(newValue))
                        .build()
        );
    }

    private String resolveActor() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest() != null) {
            String actorHeader = attributes.getRequest().getHeader("X-Actor-Username");
            if (actorHeader != null && !actorHeader.isBlank()) {
                return actorHeader.trim();
            }
            String authorization = attributes.getRequest().getHeader("Authorization");
            String fromToken = extractUsernameFromBearer(authorization);
            if (fromToken != null && !fromToken.isBlank()) {
                return fromToken.trim();
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String extractUsernameFromBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authorization.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            Map<?, ?> claims = objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), Map.class);
            Object preferred = claims.get("preferred_username");
            if (preferred instanceof String preferredUsername && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractSubjectFromBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authorization.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            Map<?, ?> claims = objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), Map.class);
            Object sub = claims.get("sub");
            return (sub instanceof String subject && !subject.isBlank()) ? subject : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> userSnapshot(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("firstName", user.getFirstName());
        map.put("lastName", user.getLastName());
        map.put("role", String.valueOf(user.getRole()));
        map.put("phone", user.getPhone());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("preferredLanguage", user.getPreferredLanguage());
        map.put("notificationsEnabled", user.isNotificationsEnabled());
        map.put("theme", user.getTheme());
        map.put("mustChangePassword", user.isMustChangePassword());
        map.put("accountStatus", String.valueOf(user.getAccountStatus()));
        map.put("enabled", user.isEnabled());
        return map;
    }

    private void notifyAccountStatusChanged(User user, Map<String, Object> before, Map<String, Object> after) {
        String accountStatus = String.valueOf(after.get("accountStatus"));
        String title;
        if ("INACTIVE".equalsIgnoreCase(accountStatus)) {
            title = "Account Deactivated";
        } else if ("ACTIVE".equalsIgnoreCase(accountStatus)) {
            title = "Account Activated";
        } else {
            title = "Account Status Updated";
        }

        String changes = buildChangesMessage(before, after);
        notificationBridgeService.pushNotification(
                "AccountStatusChanged",
                title,
                "Your account status was updated. " + changes,
                user.getId()
        );
    }

    private void notifyProfileUpdated(User user, Map<String, Object> before, Map<String, Object> after) {
        String changes = buildChangesMessage(before, after);
        notificationBridgeService.pushNotification(
                "ProfileUpdated",
                "Account Profile Updated",
                "Your account profile was updated. " + changes,
                user.getId()
        );
    }

    private String buildChangesMessage(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) {
            return "No detailed field diff available.";
        }
        List<String> changes = new java.util.ArrayList<>();
        for (String key : after.keySet()) {
            if ("id".equals(key) || "username".equals(key)) {
                continue;
            }
            Object oldVal = before.get(key);
            Object newVal = after.get(key);
            if (!valuesEqual(oldVal, newVal)) {
                changes.add(humanizeField(key) + ": " + stringifyValue(oldVal) + " -> " + stringifyValue(newVal));
            }
        }
        if (changes.isEmpty()) {
            return "No visible field changes.";
        }
        return "Changed: " + String.join(" | ", changes);
    }

    private boolean valuesEqual(Object oldVal, Object newVal) {
        return java.util.Objects.equals(oldVal, newVal);
    }

    private String stringifyValue(Object value) {
        if (value == null) return "-";
        String raw = String.valueOf(value);
        return raw.isBlank() ? "-" : raw;
    }

    private String humanizeField(String key) {
        return switch (key) {
            case "firstName" -> "First Name";
            case "lastName" -> "Last Name";
            case "dateOfBirth" -> "Date Of Birth";
            case "accountStatus" -> "Account Status";
            case "enabled" -> "Access";
            default -> key.replaceAll("([a-z])([A-Z])", "$1 $2");
        };
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase();
        return switch (normalized) {
            case "fr", "en", "ar" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported language. Allowed values: en, fr, ar.");
        };
    }

    private String normalizeTheme(String theme) {
        String normalized = theme == null ? "" : theme.trim().toLowerCase();
        return switch (normalized) {
            case "light", "dark", "system" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported theme. Allowed values: light, dark, system.");
        };
    }

    private void triggerEmailVerificationWorkflow(User user) {
        if (user == null || user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            return;
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        keycloakAdminService.ensureEmailVerificationRequired(user.getKeycloakId());
        keycloakAdminService.sendVerificationEmailIfPossible(user.getKeycloakId());
    }

    private void triggerEmailVerificationWorkflowByChange(User user, String newEmail) {
        if (user == null || user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            return;
        }
        String oldEmail = user.getEmail() == null ? "" : user.getEmail().trim();
        String normalizedNewEmail = newEmail == null ? "" : newEmail.trim();
        if (oldEmail.equalsIgnoreCase(normalizedNewEmail) || normalizedNewEmail.isBlank()) {
            return;
        }
        keycloakAdminService.ensureEmailVerificationRequired(user.getKeycloakId());
        keycloakAdminService.sendVerificationEmailIfPossible(user.getKeycloakId());
    }
}

