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

        return new AuthenticatedUser(userId, username == null ? userId : username);
    }

    @Getter
    public static class AuthenticatedUser {
        private final String userId;
        private final String username;

        public AuthenticatedUser(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}
