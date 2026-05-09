package tn.esprit.spring.userservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.spring.userservice.dto.request.ChangeMyPasswordRequest;
import tn.esprit.spring.userservice.dto.request.CreateGuardianAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateStaffAccountRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyPreferencesRequest;
import tn.esprit.spring.userservice.dto.response.MyAccountSettingsResponse;
import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.Sex;
import tn.esprit.spring.userservice.entity.User;
import tn.esprit.spring.userservice.repository.UserAuditLogRepository;
import tn.esprit.spring.userservice.repository.UserRepository;
import tn.esprit.spring.userservice.service.InternalNotificationBridgeService;
import tn.esprit.spring.userservice.service.KeycloakAdminService;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - Core Account Flows")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private InternalNotificationBridgeService notificationBridgeService;

    @Mock
    private UserAuditLogRepository userAuditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private CreateGuardianAccountRequest guardianRequest;
    private CreateStaffAccountRequest staffRequest;

    @BeforeEach
    void setUp() {
        guardianRequest = new CreateGuardianAccountRequest();
        guardianRequest.setUsername("guardian1");
        guardianRequest.setEmail("guardian1@example.com");
        guardianRequest.setCin("12345678");
        guardianRequest.setFirstName("Lea");
        guardianRequest.setLastName("Ben Ali");
        guardianRequest.setPhone("20000000");
        guardianRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guardianRequest.setSex(Sex.FEMALE);

        staffRequest = new CreateStaffAccountRequest();
        staffRequest.setUsername("staff1");
        staffRequest.setCin("87654321");
        staffRequest.setFirstName("Adam");
        staffRequest.setLastName("Mansour");
        staffRequest.setEmail("staff1@example.com");
        staffRequest.setPhone("21111111");
        staffRequest.setDateOfBirth(LocalDate.of(1988, 5, 5));
        staffRequest.setSex(Sex.MALE);
        staffRequest.setRole(Role.GUARDIAN);
    }

    @Test
    @DisplayName("createGuardian persists the account and triggers verification")
    void createGuardianPersistsAccount() {
        when(userRepository.existsByUsernameAndDeletedFalse(guardianRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse(guardianRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCinAndDeletedFalse(guardianRequest.getCin())).thenReturn(false);
        when(userRepository.existsByPhoneAndDeletedFalse(guardianRequest.getPhone())).thenReturn(false);
        when(keycloakAdminService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq(true)))
                .thenReturn("kc-guardian-1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createGuardian(guardianRequest);

        assertNotNull(response);
        assertEquals("guardian1", response.getUsername());
        assertEquals(Role.GUARDIAN, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());
        assertEquals(true, response.isEnabled());

        verify(keycloakAdminService).createUser(
                eq("guardian1"),
                eq("guardian1@example.com"),
                eq("Lea"),
                eq("Ben Ali"),
                eq("12345678"),
                eq("12345678"),
                eq(Role.GUARDIAN.name()),
                eq(true)
        );
        verify(keycloakAdminService).ensureEmailVerificationRequired("kc-guardian-1");
        verify(keycloakAdminService).sendVerificationEmailIfPossible("kc-guardian-1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createStaff rejects unsupported roles before calling Keycloak")
    void createStaffRejectsUnsupportedRole() {
        when(userRepository.existsByUsernameAndDeletedFalse(staffRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse(staffRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCinAndDeletedFalse(staffRequest.getCin())).thenReturn(false);
        when(userRepository.existsByPhoneAndDeletedFalse(staffRequest.getPhone())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.createStaff(staffRequest));

        assertEquals("Invalid staff role: GUARDIAN", exception.getMessage());
        verifyNoInteractions(keycloakAdminService);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateMyPreferences normalizes and persists account settings")
    void updateMyPreferencesPersistsChanges() {
        User authenticatedUser = baseUser(42L, "alice", "alice@example.com", Role.DOCTOR);
        when(userRepository.findByUsernameAndDeletedFalse("alice")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMyPreferencesRequest request = new UpdateMyPreferencesRequest();
        request.setPreferredLanguage("FR");
        request.setNotificationsEnabled(false);
        request.setTheme("Dark");

        MyAccountSettingsResponse response = userService.updateMyPreferences(buildBearerToken("alice"), request);

        assertNotNull(response);
        assertEquals("fr", response.getPreferredLanguage());
        assertFalse(response.isNotificationsEnabled());
        assertEquals("dark", response.getTheme());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("changeMyPassword updates the password after credential validation")
    void changeMyPasswordUpdatesPassword() {
        User authenticatedUser = baseUser(77L, "dr.samir", "samir@example.com", Role.DOCTOR);
        authenticatedUser.setMustChangePassword(true);
        when(userRepository.findByUsernameAndDeletedFalse("dr.samir")).thenReturn(Optional.of(authenticatedUser));
        when(keycloakAdminService.validateCredentials("dr.samir", "Current1!Pass")).thenReturn(true);
        doNothing().when(keycloakAdminService).updatePassword("kc-77", "NewPass1!x", false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangeMyPasswordRequest request = new ChangeMyPasswordRequest();
        request.setCurrentPassword("Current1!Pass");
        request.setNewPassword("NewPass1!x");
        request.setConfirmPassword("NewPass1!x");

        userService.changeMyPassword(buildBearerToken("dr.samir"), request);

        verify(keycloakAdminService).validateCredentials("dr.samir", "Current1!Pass");
        verify(keycloakAdminService).updatePassword("kc-77", "NewPass1!x", false);
        verify(notificationBridgeService).pushNotification(
                eq("PasswordChanged"),
                eq("Password Updated"),
                eq("Your account password was changed successfully."),
                eq(77L)
        );
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createGuardian rejects duplicate phone numbers")
    void createGuardianRejectsDuplicatePhone() {
        when(userRepository.existsByUsernameAndDeletedFalse(guardianRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse(guardianRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCinAndDeletedFalse(guardianRequest.getCin())).thenReturn(false);
        when(userRepository.existsByPhoneAndDeletedFalse(guardianRequest.getPhone())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.createGuardian(guardianRequest));

        assertEquals("Phone already exists", exception.getMessage());
        verifyNoInteractions(keycloakAdminService);
    }

    private User baseUser(Long id, String username, String email, Role role) {
        return User.builder()
                .id(id)
                .keycloakId("kc-" + id)
                .username(username)
                .cin("CIN-" + id)
                .firstName("First")
                .lastName("Last")
                .email(email)
                .phone("20000000")
                .role(role)
                .sex(Sex.MALE)
                .accountStatus(AccountStatus.ACTIVE)
                .enabled(true)
                .build();
    }

    private String buildBearerToken(String preferredUsername) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"preferred_username\":\"" + preferredUsername + "\",\"sub\":\"subject-1\"}").getBytes());
        return "Bearer " + header + "." + payload + ".signature";
    }
}
