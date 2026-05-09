package tn.esprit.spring.clinicalservice.patient;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.patient.dto.PatientProfileDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for patient operations
 * Provides endpoints for retrieving patient information
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    /**
     * List all patients
     * @return List of patient summaries
     */
    @GetMapping
    public ResponseEntity<List<PatientSummary>> getAllPatients() {
        // Return empty list for now - integrate with PatientDirectoryClient as needed
        return ResponseEntity.ok(new ArrayList<>());
    }

    /**
     * Get patient by ID
     * @param patientId Patient identifier
     * @return Patient profile details
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientProfileDetails> getPatientById(@PathVariable String patientId) {
        // Return 404 if patient not found
        return ResponseEntity.notFound().build();
    }
}
