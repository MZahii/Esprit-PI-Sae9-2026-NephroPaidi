package tn.esprit.spring.communicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;

import java.util.List;

/**
 * Feign client for User-Service communication
 * Provides methods to fetch user data (doctors, guardians)
 */
@FeignClient(
    name = "user-service",
    url = "${internal.user-service.base-url:http://localhost:8090}",
    configuration = FeignClientConfiguration.class
)
public interface UserServiceClientFeign {
    
    @GetMapping("/api/users/doctors")
    List<UserSummary> getDoctors(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/users/doctors")
    List<UserSummary> getDoctorsAlt(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/users/guardians")
    List<UserSummary> getGuardians(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/users/guardians")
    List<UserSummary> getGuardiansAlt(
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
