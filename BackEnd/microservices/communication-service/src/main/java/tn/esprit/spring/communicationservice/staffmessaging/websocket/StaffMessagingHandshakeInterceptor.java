package tn.esprit.spring.communicationservice.staffmessaging.websocket;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class StaffMessagingHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "staffMessagingUserId";
    public static final String ATTR_DISPLAY_NAME = "staffMessagingDisplayName";
    public static final String ATTR_ROLE = "staffMessagingRole";

    private final JwtDecoder jwtDecoder;

    public StaffMessagingHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = readToken(request.getURI());
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            InternalStaffRole role = resolveRole(jwt);
            if (role == null) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            String userId = trimToNull(jwt.getSubject());
            if (userId == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_ROLE, role);
            attributes.put(ATTR_DISPLAY_NAME, resolveDisplayName(jwt, userId));
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String readToken(URI uri) {
        if (uri == null || uri.getQuery() == null || uri.getQuery().isBlank()) {
            return null;
        }

        for (String part : uri.getQuery().split("&")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return trimToNull(java.net.URLDecoder.decode(keyValue[1], java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    private InternalStaffRole resolveRole(Jwt jwt) {
        return getCurrentRoles(jwt).stream()
                .map(InternalStaffRole::fromValue)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Set<String> getCurrentRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            addRoles(roles, realmAccessMap.get("roles"));
        }

        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
            for (Object value : resourceAccessMap.values()) {
                if (value instanceof Map<?, ?> clientAccessMap) {
                    addRoles(roles, clientAccessMap.get("roles"));
                }
            }
        }

        return roles;
    }

    private void addRoles(Set<String> target, Object candidate) {
        if (!(candidate instanceof Collection<?> collection)) {
            return;
        }

        for (Object value : collection) {
            if (value instanceof String role) {
                String normalized = role.trim().toUpperCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    target.add(normalized);
                }
            }
        }
    }

    private String resolveDisplayName(Jwt jwt, String fallbackUserId) {
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

        String username = trimToNull(claimAsString(jwt, "preferred_username"));
        return username != null ? username : fallbackUserId;
    }

    private String claimAsString(Jwt jwt, String claimName) {
        Object claim = jwt.getClaims().get(claimName);
        return claim instanceof String stringClaim ? stringClaim : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
