package tn.esprit.spring.communicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import tn.esprit.spring.communicationservice.integration.dto.AdministrationPatientProfile;

import java.util.List;

/**
 * Feign client for Administration-Service communication
 * Provides methods to fetch patient profile data
 */
@FeignClient(
    name = "administration-service",
    url = "${internal.administration-service.base-url:http://localhost:8087}",
    configuration = FeignClientConfiguration.class
)
public interface AdministrationServiceClientFeign {
    
    @GetMapping("/api/patients/guardian/{guardianUserId}")
    List<AdministrationPatientProfile> getPatientsByGuardianId(
        @PathVariable Long guardianUserId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
