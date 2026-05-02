package tn.esprit.spring.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.userservice.config.KeycloakAdminConfig;
import tn.esprit.spring.userservice.dto.request.LoginRequest;
import tn.esprit.spring.userservice.dto.request.ResendVerificationEmailRequest;
import tn.esprit.spring.userservice.dto.response.TokenRefreshResponse;
import tn.esprit.spring.userservice.dto.response.KeycloakTokenResponse;
import tn.esprit.spring.userservice.dto.response.LoginResponse;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.User;
import tn.esprit.spring.userservice.repository.UserRepository;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final KeycloakAdminConfig keycloakConfig;
    private final RestTemplateBuilder restTemplateBuilder;
    private final KeycloakAdminService keycloakAdminService;

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(
                        UNAUTHORIZED,
                        "Invalid username/email or password"
                ));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "User account is disabled");
        }
        enforceEmailVerification(user);

        KeycloakTokenResponse tokenResponse = requestTokenFromKeycloak(
                user.getUsername(),
                request.getPassword()
        );

        String redirectTo = user.isMustChangePassword()
                ? "/backoffice/account-settings?forcePasswordChange=true"
                : resolveRedirect(user.getRole());

        return LoginResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .role(user.getRole().name())
                .redirectTo(redirectTo)
                .userId(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    public TokenRefreshResponse refresh(String refreshToken) {
        String keycloakId = extractSubjectFromToken(refreshToken);
        if (keycloakId != null) {
            userRepository.findByKeycloakIdAndDeletedFalse(keycloakId)
                    .ifPresent(this::enforceEmailVerification);
        }

        KeycloakTokenResponse tokenResponse = refreshTokenFromKeycloak(refreshToken);
        return TokenRefreshResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .build();
    }

    public void resendVerificationEmail(ResendVerificationEmailRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        if (identifier.isBlank()) {
            return;
        }

        userRepository.findByIdentifier(identifier).ifPresent(user -> {
            KeycloakAdminService.KeycloakUserState state = keycloakAdminService.getUserState(user.getKeycloakId());
            if (!state.hasEmail() || state.emailVerified()) {
                return;
            }
            keycloakAdminService.ensureEmailVerificationRequired(user.getKeycloakId());
            keycloakAdminService.sendVerificationEmailIfPossible(user.getKeycloakId());
        });
    }

    private KeycloakTokenResponse requestTokenFromKeycloak(String username, String password) {
        String tokenUrl = keycloakConfig.getServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakConfig.getAuth().getClientId());
        form.add("client_secret", keycloakConfig.getAuth().getClientSecret());
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    KeycloakTokenResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(
                        UNAUTHORIZED,
                        "Invalid username/email or password"
                );
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Invalid username/email or password"
            );
        }
    }

    private KeycloakTokenResponse refreshTokenFromKeycloak(String refreshToken) {
        String tokenUrl = keycloakConfig.getServerUrl()
                + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakConfig.getAuth().getClientId());
        form.add("client_secret", keycloakConfig.getAuth().getClientSecret());
        form.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    KeycloakTokenResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(
                        UNAUTHORIZED,
                        "Session expired. Please login again."
                );
            }

            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Session expired. Please login again."
            );
        }
    }

    private String resolveRedirect(Role role) {
        return switch (role) {
            case ADMIN, HR, DOCTOR, NURSE, LAB_AGENT, SURGEON, PHARMACIST, RECEPTIONIST -> "/backoffice/dashboard";
            case GUARDIAN -> "/frontoffice/home";
        };
    }

    private void enforceEmailVerification(User user) {
        KeycloakAdminService.KeycloakUserState keycloakState = keycloakAdminService.getUserState(user.getKeycloakId());

        if (!keycloakState.hasEmail()) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Email address is required before account access can be granted. Please contact an administrator."
            );
        }

        if (!keycloakState.emailVerified()) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Email is not verified. Please verify your email before accessing the application."
            );
        }
    }

    private String extractSubjectFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int subIndex = payload.indexOf("\"sub\"");
            if (subIndex < 0) {
                return null;
            }
            int colonIndex = payload.indexOf(':', subIndex);
            int firstQuote = payload.indexOf('"', colonIndex + 1);
            int secondQuote = payload.indexOf('"', firstQuote + 1);
            if (firstQuote < 0 || secondQuote < 0) {
                return null;
            }
            return payload.substring(firstQuote + 1, secondQuote);
        } catch (Exception ex) {
            return null;
        }
    }
}
