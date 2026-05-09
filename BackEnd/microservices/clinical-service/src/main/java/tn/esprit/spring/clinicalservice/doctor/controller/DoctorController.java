package tn.esprit.spring.clinicalservice.doctor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.client.UserServiceClient;

import java.util.UUID;

/**
 * REST Controller for doctor-related operations
 * Provides endpoints for retrieving doctor information
 */
@RestController
@RequestMapping("/clinical/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final UserServiceClient userServiceClient;

    /**
     * Get doctor details by ID
     * @param id Doctor ID (UUID)
     * @param authentication Authentication context
     * @return Doctor information
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getDoctorById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        try {
            String token = extractBearerToken(authentication);
            var response = userServiceClient.getUserById(id.toString(), token);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found: " + id);
            }
            return response;
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException) {
                throw ex;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve doctor information", ex);
        }
    }

    /**
     * Search doctors by name or specialty
     * @param query Search query
     * @param authentication Authentication context
     * @return List of matching doctors
     */
    @GetMapping("/search")
    public ResponseEntity<Object> searchDoctors(
            @RequestParam("q") String query,
            Authentication authentication
    ) {
        try {
            String token = extractBearerToken(authentication);
            return userServiceClient.searchStaff(query, token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search doctors", ex);
        }
    }

    private String extractBearerToken(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            String token = (String) authentication.getPrincipal();
            return "Bearer " + token;
        }
        return null;
    }
}
