package tn.esprit.spring.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.userservice.dto.response.UserResponse;
import tn.esprit.spring.userservice.entity.AccountStatus;
import tn.esprit.spring.userservice.service.UserService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PutMapping("/{userId}/activation")
    public UserResponse updateActivation(
            @PathVariable Long userId,
            @RequestParam boolean enabled,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }

        return userService.updateActivation(userId, enabled);
    }

    @PatchMapping("/{userId}/activation")
    public UserResponse updateActivationPatch(
            @PathVariable Long userId,
            @RequestParam boolean enabled,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        return updateActivation(userId, enabled, apiKey);
    }

    @PutMapping("/{userId}/status")
    public UserResponse updateStatus(
            @PathVariable Long userId,
            @RequestParam AccountStatus status,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }

        return userService.updateAccountStatus(userId, status);
    }

    @PatchMapping("/{userId}/status")
    public UserResponse updateStatusPatch(
            @PathVariable Long userId,
            @RequestParam AccountStatus status,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        return updateStatus(userId, status, apiKey);
    }

    @GetMapping("/{userId}/summary")
    public UserResponse getUserSummary(
            @PathVariable Long userId,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
        try {
            return userService.getUserById(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/pending-contract-count")
    public long countPendingContractOlderThan(
            @RequestParam(defaultValue = "7") int days,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
        return userService.countPendingUsersOlderThanDays(days);
    }
}
