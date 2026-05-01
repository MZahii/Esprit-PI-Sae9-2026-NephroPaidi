package tn.esprit.spring.procedureservice.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/procedures/whatsapp/test").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/procedures/**")
                    .hasAnyRole("SURGEON", "DOCTOR", "NURSE", "GUARDIAN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/api/procedures/surgical/requests")
                    .hasAnyRole("DOCTOR", "SURGEON")
                .requestMatchers(HttpMethod.POST, "/api/procedures/surgical/cases")
                    .hasAnyRole("SURGEON", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT, "/api/procedures/surgical/requests/*/status")
                    .hasAnyRole("SURGEON", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/api/procedures/**").hasRole("SURGEON")
                .requestMatchers(HttpMethod.PUT, "/api/procedures/**").hasRole("SURGEON")
                .requestMatchers(HttpMethod.PATCH, "/api/procedures/**").hasRole("SURGEON")
                .requestMatchers(HttpMethod.DELETE, "/api/procedures/**").hasRole("SURGEON")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        addRolesFromClaim(authorities, realmAccess);

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Object clientAccess : resourceAccess.values()) {
                if (clientAccess instanceof Map<?, ?> clientRoles) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>) clientRoles;
                    addRolesFromClaim(authorities, casted);
                }
            }
        }

        return authorities;
    }

    private void addRolesFromClaim(Collection<GrantedAuthority> authorities, Map<String, Object> accessClaim) {
        if (accessClaim == null || !accessClaim.containsKey("roles")) {
            return;
        }

        Object rolesObject = accessClaim.get("roles");
        if (rolesObject instanceof Collection<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj == null) {
                    continue;
                }
                String normalizedRole = roleObj.toString().trim().toUpperCase(Locale.ROOT);
                if (!normalizedRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
                }
            }
        }
    }
}
