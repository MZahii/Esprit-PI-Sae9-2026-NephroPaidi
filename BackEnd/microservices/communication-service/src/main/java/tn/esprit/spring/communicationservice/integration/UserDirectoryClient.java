package tn.esprit.spring.communicationservice.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.spring.communicationservice.client.UserServiceClientFeign;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserDirectoryClient {

    private final UserServiceClientFeign userServiceClientFeign;

    public UserSummary resolveGuardian(String jwtSub, String preferredUsername) {
        String token = currentAuthorizationHeader();
        List<UserSummary> guardians = tryLoadGuardians(token);

        if (guardians == null || guardians.isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404), "No guardian users found in user-service");
        }

        return guardians.stream()
                .filter(Objects::nonNull)
                .filter(user -> isMatch(user, jwtSub, preferredUsername))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "Failed to resolve guardian user in user-service"));
    }

    public List<UserSummary> loadDoctors() {
        String token = currentAuthorizationHeader();
        try {
            return userServiceClientFeign.getDoctors(token);
        } catch (ResponseStatusException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                try {
                    return userServiceClientFeign.getDoctorsAlt(token);
                } catch (ResponseStatusException altEx) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to load doctors from user-service");
                }
            }
            throw ex;
        }
    }

    private List<UserSummary> tryLoadGuardians(String token) {
        try {
            return userServiceClientFeign.getGuardians(token);
        } catch (ResponseStatusException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                try {
                    return userServiceClientFeign.getGuardiansAlt(token);
                } catch (ResponseStatusException altEx) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to load guardians from user-service");
                }
            }
            throw ex;
        }
    }

    private List<UserSummary> fetchGuardians(String endpoint) {
        return tryLoadGuardians(currentAuthorizationHeader());
    }

    private List<UserSummary> fetchUsers(String endpoint, String label) {
        if (endpoint.contains("doctors")) {
            return loadDoctors();
        }
        return tryLoadGuardians(currentAuthorizationHeader());
    }

    private boolean isMatch(UserSummary user, String jwtSub, String preferredUsername) {
        if (equalsIgnoreCaseSafe(user.getKeycloakId(), jwtSub)) {
            return true;
        }

        return equalsIgnoreCaseSafe(user.getUsername(), preferredUsername);
    }

    private boolean equalsIgnoreCaseSafe(String left, String right) {
        if (left == null || right == null) {
            return false;
        }

        return left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return null;
        }

        String authorization = attributes.getRequest().getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        return authorization.trim();
    }
}
