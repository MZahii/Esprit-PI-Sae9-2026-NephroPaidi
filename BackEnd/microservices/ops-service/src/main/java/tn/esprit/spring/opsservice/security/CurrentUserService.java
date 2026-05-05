package tn.esprit.spring.opsservice.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalStateException("Authenticated JWT user is required");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("name");
        }

        String displayName = jwt.getClaimAsString("name");
        if (displayName == null || displayName.isBlank()) {
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String combined = ((givenName == null ? "" : givenName.trim()) + " " + (familyName == null ? "" : familyName.trim())).trim();
            displayName = combined.isBlank() ? null : combined;
        }

        String resolvedUsername = username == null || username.isBlank() ? userId : username;
        String resolvedDisplayName = displayName == null || displayName.isBlank() ? resolvedUsername : displayName;

        return new AuthenticatedUser(userId, resolvedUsername, resolvedDisplayName);
    }

    @Getter
    public static class AuthenticatedUser {
        private final String userId;
        private final String username;
        private final String displayName;

        public AuthenticatedUser(String userId, String username, String displayName) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
        }
    }
}
