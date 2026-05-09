package tn.esprit.spring.clinicalservice.patient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.esprit.spring.clinicalservice.patient.dto.PatientSummary;
import tn.esprit.spring.clinicalservice.patient.dto.PatientProfileDetails;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientDirectoryClient {

    private final RestTemplate restTemplate;

    @Value("${services.administration.base-url:http://localhost:8087}")
    private String administrationBase;

    public Map<Long, PatientSummary> getPatientsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        String joinedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        if (joinedIds.isEmpty()) {
            return Map.of();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(administrationBase)
                .path("/api/patients/batch")
                .queryParam("ids", joinedIds)
                .build(true)
                .toUri();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<PatientSummary[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, PatientSummary[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Map.of();
            }
            Map<Long, PatientSummary> result = new HashMap<>();
            for (PatientSummary summary : response.getBody()) {
                if (summary != null && summary.getId() != null) {
                    result.put(summary.getId(), summary);
                }
            }
            return result;
        } catch (RestClientException ex) {
            log.warn("Patient batch lookup failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    public List<Long> searchPatientIds(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(administrationBase)
                .path("/api/patients/search")
                .queryParam("q", trimmed)
                .queryParam("limit", Math.min(Math.max(limit, 1), 20))
                .build(true)
                .toUri();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<PatientSummary[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, PatientSummary[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }
            return Arrays.stream(response.getBody())
                    .filter(Objects::nonNull)
                    .map(PatientSummary::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RestClientException ex) {
            log.warn("Patient search lookup failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<Long> getPatientIdsByGuardianUserId(Long guardianUserId) {
        if (guardianUserId == null) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(administrationBase)
                .path("/api/patients/guardian/{guardianUserId}")
                .buildAndExpand(guardianUserId)
                .toUri();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<PatientProfileDetails[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, PatientProfileDetails[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }
            return Arrays.stream(response.getBody())
                    .filter(Objects::nonNull)
                    .map(PatientProfileDetails::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (RestClientException ex) {
            log.warn("Guardian patient lookup failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public Long getGuardianUserIdForPatient(Long patientId) {
        if (patientId == null) {
            return null;
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(administrationBase)
                .path("/api/patients/{patientId}")
                .buildAndExpand(patientId)
                .toUri();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<PatientProfileDetails> response = restTemplate.exchange(uri, HttpMethod.GET, entity, PatientProfileDetails.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            return response.getBody().getGuardianUserId();
        } catch (RestClientException ex) {
            log.warn("Guardian lookup by patient failed: {}", ex.getMessage());
            return null;
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String token = jwtToken.getToken().getTokenValue();
            if (token != null && !token.isBlank()) {
                headers.setBearerAuth(token);
            }
        }
        return headers;
    }
}
