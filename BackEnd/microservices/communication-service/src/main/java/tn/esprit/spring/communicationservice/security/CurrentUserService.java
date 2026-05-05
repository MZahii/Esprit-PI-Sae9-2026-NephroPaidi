package tn.esprit.spring.communicationservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentUserService {

    private static final Set<String> STAFF_ROLES = Set.of("RECEPTIONIST", "NURSE", "DOCTOR");
    private static final Set<String> INTERNAL_STAFF_MESSAGING_ROLES = Set.of(
            "ADMIN",
            "HR",
            "DOCTOR",
            "NURSE",
            "RECEPTIONIST",
            "PHARMACIST",
            "LAB_AGENT",
            "SURGEON"
    );
    private static final List<String> INTERNAL_STAFF_ROLE_PRIORITY = List.of(
            "ADMIN",
            "HR",
            "DOCTOR",
            "NURSE",
            "RECEPTIONIST",
            "PHARMACIST",
            "LAB_AGENT",
            "SURGEON"
    );

    public String getCurrentUserSub() {
        Jwt jwt = currentJwt();
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new AccessDeniedException("JWT subject is missing");
        }
        return sub;
    }

    public String getPreferredUsernameOrNull() {
        Jwt jwt = currentJwt();
        Object preferredUsername = jwt.getClaims().get("preferred_username");
        if (!(preferredUsername instanceof String username)) {
            return null;
        }

        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Set<String> getCurrentRoles() {
        Jwt jwt = currentJwt();
        Object realmAccess = jwt.getClaims().get("realm_access");

        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Collections.emptySet();
        }

        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return Collections.emptySet();
        }

        return roleCollection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String role) {
        return getCurrentRoles().contains(role.toUpperCase());
    }

    public void requireRole(String role) {
        if (!hasRole(role)) {
            throw new AccessDeniedException("Role " + role + " is required");
        }
    }

    public boolean isGuardian() {
        return hasRole("GUARDIAN");
    }

    public String getDisplayNameOrUsername() {
        Jwt jwt = currentJwt();
        String fullName = trimToNull(claimAsString(jwt, "name"));
        if (fullName != null) {
            return fullName;
        }

        String givenName = trimToNull(claimAsString(jwt, "given_name"));
        String familyName = trimToNull(claimAsString(jwt, "family_name"));
        String combined = trimToNull(((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName)).trim());
        if (combined != null) {
            return combined;
        }

        String preferredUsername = getPreferredUsernameOrNull();
        if (preferredUsername != null) {
            return preferredUsername;
        }

        return getCurrentUserSub();
    }

    public InternalStaffRole getInternalStaffMessagingRoleOrThrow() {
        Set<String> roles = getCurrentRoles();
        for (String role : INTERNAL_STAFF_ROLE_PRIORITY) {
            if (roles.contains(role)) {
                return InternalStaffRole.valueOf(role);
            }
        }

        throw new AccessDeniedException("Internal staff messaging requires a staff role");
    }

    public StaffRole getStaffRoleOrThrow() {
        Set<String> roles = getCurrentRoles();
        List<String> presentStaffRoles = roles.stream()
                .filter(STAFF_ROLES::contains)
                .toList();

        if (presentStaffRoles.isEmpty()) {
            throw new AccessDeniedException("Staff role is required");
        }

        return StaffRole.valueOf(presentStaffRoles.get(0));
    }

    public MessageQueue getStaffQueueOrThrow() {
        return MessageQueue.valueOf(getStaffRoleOrThrow().name());
    }

    public SenderRole getSenderRoleOrThrow() {
        if (isGuardian()) {
            return SenderRole.GUARDIAN;
        }

        return SenderRole.valueOf(getStaffRoleOrThrow().name());
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("JWT authentication is required");
        }
        return jwt;
    }

    private String claimAsString(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        return value instanceof String stringValue ? stringValue : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
