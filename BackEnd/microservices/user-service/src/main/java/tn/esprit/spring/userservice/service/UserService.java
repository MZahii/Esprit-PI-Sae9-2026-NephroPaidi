package tn.esprit.spring.userservice.service;

import tn.esprit.spring.userservice.dto.request.CreateGuardianAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateHrAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateInternalUserRequest;
import tn.esprit.spring.userservice.dto.request.CreateStaffAccountRequest;
import tn.esprit.spring.userservice.dto.request.StaffSearchRequest;
import tn.esprit.spring.userservice.dto.request.UpdateGuardianProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateHrProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateStaffProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyPreferencesRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyProfileRequest;
import tn.esprit.spring.userservice.dto.request.ChangeMyPasswordRequest;
import tn.esprit.spring.userservice.dto.response.MyAccountSettingsResponse;
import tn.esprit.spring.userservice.dto.response.StaffSearchResponse;
import tn.esprit.spring.userservice.dto.response.UserAuditLogResponse;
import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.entity.AccountStatus;

import java.util.List;

public interface UserService {
    UserResponse createHr(CreateHrAccountRequest request);
    UserResponse createInternalUser(CreateInternalUserRequest request);
    UserResponse createStaff(CreateStaffAccountRequest request);
    UserResponse createGuardian(CreateGuardianAccountRequest request);
    List<UserResponse> getAllUsers();
    List<UserResponse> getGuardians();
    List<UserResponse> getDoctors();

    UserResponse updateActivation(Long userId, boolean enabled);
    UserResponse updateAccountStatus(Long userId, AccountStatus accountStatus);
    UserResponse updateStaffProfile(Long userId, UpdateStaffProfileRequest request);
    UserResponse updateHrProfile(Long userId, UpdateHrProfileRequest request);
    UserResponse updateGuardianProfile(Long userId, UpdateGuardianProfileRequest request);
    UserResponse getUserById(Long userId);
    List<UserAuditLogResponse> getUserAuditLogs(Long userId);
    List<UserAuditLogResponse> getAllUserAuditLogs();
    StaffSearchResponse searchStaff(StaffSearchRequest request);
    long countPendingUsersOlderThanDays(int days);
    UserResponse softDeleteUser(Long userId);
    UserResponse restoreUser(Long userId);
    MyAccountSettingsResponse getMySettings(String authorization);
    MyAccountSettingsResponse updateMyProfile(String authorization, UpdateMyProfileRequest request);
    MyAccountSettingsResponse updateMyPreferences(String authorization, UpdateMyPreferencesRequest request);
    void changeMyPassword(String authorization, ChangeMyPasswordRequest request);
}
