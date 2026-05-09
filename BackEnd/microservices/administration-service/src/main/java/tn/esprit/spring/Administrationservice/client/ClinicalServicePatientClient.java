package tn.esprit.spring.Administrationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Feign Client to proxy clinical service patient endpoints
 */
@FeignClient(name = "clinical-service", url = "${CLINICAL_SERVICE_URL:http://localhost:8090}")
public interface ClinicalServicePatientClient {

    @GetMapping("/api/patients")
    ResponseEntity<List<Map<String, Object>>> getAllPatients();

    @GetMapping("/api/patients/{patientId}")
    ResponseEntity<Map<String, Object>> getPatientById(@PathVariable String patientId);
}
