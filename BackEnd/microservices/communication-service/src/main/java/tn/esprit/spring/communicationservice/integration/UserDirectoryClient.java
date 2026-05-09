package tn.esprit.spring.communicationservice.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.spring.communicationservice.client.UserServiceClientFeign;
import tn.esprit.spring.communicationservice.integration.dto.StaffSearchRequest;
import tn.esprit.spring.communicationservice.integration.dto.StaffSearchResponse;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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

    public List<UserSummary> searchStaffUsers(String query, List<InternalStaffRole> roles, int limit) {
        StaffSearchRequest request = new StaffSearchRequest();
        request.setQuery(query);
        request.setRoles(roles.stream().map(Enum::name).toList());
        request.setEnabled(Boolean.TRUE);
        request.setPage(0);
        request.setSize(limit);
        request.setSortBy("firstName");
        request.setSortDir("asc");

        String token = currentAuthorizationHeader();
        StaffSearchResponse response = userServiceClientFeign.searchStaff(token, request);
        return response == null || response.getItems() == null ? List.of() : response.getItems();
    }

    public UserSummary resolveStaffUser(String userId, Set<InternalStaffRole> allowedRoles) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target staff user is required");
        }

        List<UserSummary> candidates = searchStaffUsers(null, List.copyOf(allowedRoles), 200);
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(UserSummary::isEnabled)
                .filter(user -> allowedRoles.contains(InternalStaffRole.fromValue(user.getRole())))
                .filter(user -> equalsIgnoreCaseSafe(user.getKeycloakId(), userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found: " + userId));
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
