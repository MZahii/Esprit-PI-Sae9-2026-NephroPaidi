package tn.esprit.spring.clinicalservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableWebSecurity
@Profile("!local")
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/clinical/audit/**", "/api/clinical/audit/**").hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/clinical/guardian/**").hasRole("GUARDIAN")
                        .requestMatchers(HttpMethod.GET, "/clinical/appointments/**").hasAnyRole("DOCTOR", "RECEPTIONIST", "GUARDIAN")
                        .requestMatchers(HttpMethod.POST, "/clinical/appointments/*/start-consultation").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/clinical/appointments").hasAnyRole("RECEPTIONIST", "DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/clinical/appointments/*").hasRole("RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/clinical/appointments/*/cancel").hasRole("RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/clinical/consultations/*/metrics").hasAnyRole("DOCTOR", "PHARMACIST")
                        .requestMatchers("/clinical/consultations/**").hasRole("DOCTOR")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<org.springframework.security.oauth2.jwt.Jwt, Collection<GrantedAuthority>> authoritiesConverter =
                this::extractAuthorities;

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(org.springframework.security.oauth2.jwt.Jwt jwt) {
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