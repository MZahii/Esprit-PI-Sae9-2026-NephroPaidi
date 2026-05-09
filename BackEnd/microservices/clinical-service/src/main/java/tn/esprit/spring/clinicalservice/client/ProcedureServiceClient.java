package tn.esprit.spring.clinicalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for Procedure-Service communication
 * Provides methods to fetch surgical and dialysis procedure data
 */
@FeignClient(
    name = "procedure-service",
    url = "${services.procedure.url:http://localhost:8089}",
    configuration = FeignClientConfiguration.class
)
public interface ProcedureServiceClient {
    
    @GetMapping("/api/procedures/surgical/cases/{id}")
    ResponseEntity<Object> getSurgicalCase(
        @PathVariable UUID id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/procedures/surgical/cases")
    ResponseEntity<List<Object>> getAllSurgicalCases(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/procedures/dialysis/sessions/{id}")
    ResponseEntity<Object> getDialysisSession(
        @PathVariable UUID id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/procedures/dialysis/sessions")
    ResponseEntity<List<Object>> getAllDialysisSessions(
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/api/procedures/dialysis/plans/{id}")
    ResponseEntity<Object> getDialysisPlan(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
