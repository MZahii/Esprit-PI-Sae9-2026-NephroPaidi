package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Feign client for Administration-Service communication
 * Provides methods to fetch patient and contract data from admin-service
 */
@FeignClient(
    name = "administration-service",
    url = "${services.admin.url:http://localhost:8085}",
    configuration = FeignClientConfiguration.class
)
public interface AdministrationClient {
    
    @GetMapping("/patients/{patientId}")
    ResponseEntity<Object> getPatient(
        @PathVariable String patientId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/patients")
    ResponseEntity<List<Object>> getAllPatients(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/contracts/{contractId}")
    ResponseEntity<Object> getContract(
        @PathVariable String contractId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/contracts")
    ResponseEntity<List<Object>> getContractsByStaff(
        @RequestParam String staffUserId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/patients/guardian/{guardianId}")
    ResponseEntity<List<Object>> getPatientsByGuardian(
        @PathVariable String guardianId,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
