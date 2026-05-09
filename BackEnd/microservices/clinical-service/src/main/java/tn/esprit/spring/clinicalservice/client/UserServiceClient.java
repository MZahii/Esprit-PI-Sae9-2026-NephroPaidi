package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Feign client for User-Service communication
 * Provides methods to fetch user data from user-service
 */
@FeignClient(
    name = "user-service",
    url = "${services.user.url:http://localhost:8086}",
    configuration = FeignClientConfiguration.class
)
public interface UserServiceClient {
    
    @GetMapping("/users/{userId}")
    ResponseEntity<Object> getUserById(
        @PathVariable String userId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @PostMapping("/users/staff/search")
    ResponseEntity<Object> searchStaff(
        @RequestBody Object request,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/users")
    ResponseEntity<List<Object>> getAllUsers(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/users/guardians")
    ResponseEntity<List<Object>> getGuardians(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/users/{userId}/audit")
    ResponseEntity<Object> getUserAuditLogs(
        @PathVariable String userId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
