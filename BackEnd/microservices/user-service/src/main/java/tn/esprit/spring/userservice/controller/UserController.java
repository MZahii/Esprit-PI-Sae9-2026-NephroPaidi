package tn.esprit.spring.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.userservice.dto.request.CreateGuardianAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateHrAccountRequest;
import tn.esprit.spring.userservice.dto.request.CreateInternalUserRequest;
import tn.esprit.spring.userservice.dto.request.CreateStaffAccountRequest;
import tn.esprit.spring.userservice.dto.request.UpdateGuardianProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateHrProfileRequest;
import tn.esprit.spring.userservice.dto.request.StaffSearchRequest;
import tn.esprit.spring.userservice.dto.request.UpdateStaffProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyProfileRequest;
import tn.esprit.spring.userservice.dto.request.UpdateMyPreferencesRequest;
import tn.esprit.spring.userservice.dto.request.ChangeMyPasswordRequest;
import tn.esprit.spring.userservice.dto.response.MyAccountSettingsResponse;
import tn.esprit.spring.userservice.dto.response.StaffSearchResponse;
import tn.esprit.spring.userservice.dto.response.UserAuditLogResponse;
import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/hr")
    public UserResponse createHr(@Valid @RequestBody CreateHrAccountRequest request) {
        return userService.createHr(request);
    }

    @PostMapping("/internal")
    public UserResponse createInternal(@Valid @RequestBody CreateInternalUserRequest request) {
        return userService.createInternalUser(request);
    }

    @PostMapping("/staff")
    public UserResponse createStaff(@Valid @RequestBody CreateStaffAccountRequest request) {
        return userService.createStaff(request);
    }

    @PatchMapping("/staff/{userId}")
    public UserResponse updateStaffProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStaffProfileRequest request
    ) {
        return userService.updateStaffProfile(userId, request);
    }

    @PatchMapping("/hr/{userId}")
    public UserResponse updateHrProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateHrProfileRequest request
    ) {
        return userService.updateHrProfile(userId, request);
    }

    @PatchMapping("/guardian/{userId}")
    public UserResponse updateGuardianProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateGuardianProfileRequest request
    ) {
        return userService.updateGuardianProfile(userId, request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/guardians")
    public List<UserResponse> getGuardians() {
        return userService.getGuardians();
    }

    @GetMapping("/public/doctors")
    public List<UserResponse> getPublicDoctors() {
        return userService.getDoctors();
    }

    @PostMapping("/staff/search")
    public StaffSearchResponse searchStaff(@RequestBody StaffSearchRequest request) {
        return userService.searchStaff(request);
    }

    @GetMapping("/{userId}/audit")
    public List<UserAuditLogResponse> getUserAuditLogs(@PathVariable Long userId) {
        return userService.getUserAuditLogs(userId);
    }

    @GetMapping("/audit")
    public List<UserAuditLogResponse> getAllUserAuditLogs() {
        return userService.getAllUserAuditLogs();
    }

    @GetMapping("/me/settings")
    public MyAccountSettingsResponse getMySettings(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return userService.getMySettings(authorization);
    }

    @PatchMapping("/me/profile")
    public MyAccountSettingsResponse updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return userService.updateMyProfile(authorization, request);
    }

    @PatchMapping("/me/preferences")
    public MyAccountSettingsResponse updateMyPreferences(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateMyPreferencesRequest request
    ) {
        return userService.updateMyPreferences(authorization, request);
    }

    @PostMapping("/me/change-password")
    public void changeMyPassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChangeMyPasswordRequest request
    ) {
        userService.changeMyPassword(authorization, request);
    }

    @PostMapping("/guardian")
    public UserResponse createGuardian(@Valid @RequestBody CreateGuardianAccountRequest request) {
        return userService.createGuardian(request);
    }

    @PatchMapping("/{userId}/soft-delete")
    public UserResponse softDelete(@PathVariable Long userId) {
        return userService.softDeleteUser(userId);
    }

    @PatchMapping("/{userId}/restore")
    public UserResponse restore(@PathVariable Long userId) {
        return userService.restoreUser(userId);
    }
}
