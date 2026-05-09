package tn.esprit.spring.clinicalservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/clinical")
public class ClinicalProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.administration.base-url:http://localhost:8087}")
    private String administrationBase;

    public ClinicalProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Proxy to administration-service patient profile
    @GetMapping("/patients/{id}")
    public ResponseEntity<String> getPatientProfile(@PathVariable("id") Long id, Authentication authentication) {
        String url = administrationBase + "/api/patients/" + id;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(authentication)), String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    private HttpHeaders authHeaders(Authentication authentication) {
        HttpHeaders headers = new HttpHeaders();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String token = jwtToken.getToken().getTokenValue();
            if (token != null && !token.isBlank()) {
                headers.setBearerAuth(token);
            }
        }
        return headers;
    }
}
