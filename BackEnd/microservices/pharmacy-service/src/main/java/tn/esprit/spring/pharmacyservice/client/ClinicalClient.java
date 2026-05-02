package tn.esprit.spring.pharmacyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "clinical-service",
    configuration = FeignClientConfiguration.class
)
public interface ClinicalClient {
    
    @GetMapping("/clinical/consultations/{id}")
    ResponseEntity<Object> getConsultation(
        @PathVariable String id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
    
    @GetMapping("/clinical/appointments/{id}")
    ResponseEntity<Object> getAppointment(
        @PathVariable String id,
        @RequestHeader(value = "Authorization", required = false) String token
    );
}
