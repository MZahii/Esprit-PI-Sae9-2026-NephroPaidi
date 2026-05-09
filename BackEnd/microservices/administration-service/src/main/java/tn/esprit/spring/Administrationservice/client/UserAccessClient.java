package tn.esprit.spring.Administrationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-service", url = "${user-service.base-url:http://localhost:8090}")
public interface UserAccessClient {

    @PutMapping("/internal/users/{userId}/activation")
    UserResponse updateActivation(
            @PathVariable("userId") Long userId,
            @RequestParam("enabled") boolean enabled,
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestHeader(value = "X-Actor-Username", required = false) String actorUsername
    );

    @PutMapping("/internal/users/{userId}/status")
    UserResponse updateStatus(
            @PathVariable("userId") Long userId,
            @RequestParam("status") String status,
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestHeader(value = "X-Actor-Username", required = false) String actorUsername
    );

    @GetMapping("/internal/users/{userId}/summary")
    InternalUserSummary getUserSummary(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );

    @GetMapping("/internal/users/pending-contract-count")
    long getPendingContractCountOlderThanDays(
            @RequestParam("days") int days,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}
