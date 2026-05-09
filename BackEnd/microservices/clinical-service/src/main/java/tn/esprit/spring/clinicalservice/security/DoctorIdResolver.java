package tn.esprit.spring.clinicalservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DoctorIdResolver {

    public UUID resolve(UUID headerId, Authentication authentication) {
        UUID fromToken = resolveFromToken(authentication);
        if (fromToken != null) {
            return fromToken;
        }

        if (headerId != null) {
            return headerId;
        }

        return null;
    }

    private UUID resolveFromToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String subject = jwtToken.getToken().getSubject();
            if (subject != null) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        return null;
    }
}
