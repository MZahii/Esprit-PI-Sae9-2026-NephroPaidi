package tn.esprit.spring.clinicalservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

@Component
public class ActorResolver {

    public ActorInfo resolveCurrent() {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }

    public ActorInfo resolve(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt token = jwtToken.getToken();
            String subject = token.getSubject();
            String username = token.getClaimAsString("preferred_username");
            String role = extractPrimaryRole(token);
            return new ActorInfo(subject, username, role);
        }

        return new ActorInfo(null, null, null);
    }

    private String extractPrimaryRole(Jwt token) {
        String role = extractFromClaim(token.getClaim("realm_access"));
        if (role != null) {
            return role;
        }

        Object resourceAccess = token.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            for (Object entry : resources.values()) {
                if (entry instanceof Map<?, ?> clientAccess) {
                    role = extractFromClaim(clientAccess);
                    if (role != null) {
                        return role;
                    }
                }
            }
        }

        return null;
    }

    private String extractFromClaim(Object claim) {
        if (!(claim instanceof Map<?, ?> map)) {
            return null;
        }
        Object rolesObj = map.get("roles");
        if (rolesObj instanceof Collection<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj == null) {
                    continue;
                }
                String normalized = roleObj.toString().trim().toUpperCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        return null;
    }
}
