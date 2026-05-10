package tn.esprit.spring.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/refresh").permitAll()
                        .pathMatchers("/api/auth/resend-verification").permitAll()
                        .pathMatchers("/api/auth/forgot-password").permitAll()
                        .pathMatchers("/swagger/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/users/public/doctors").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/core-ops/health", "/api/core-ops/rabbitmq/health").permitAll()
                        .pathMatchers("/ops/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/users/hr").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/users/staff/search").hasAnyRole("ADMIN", "HR", "RECEPTIONIST", "SURGEON", "PHARMACIST", "DOCTOR", "NURSE")
                        .pathMatchers(HttpMethod.PATCH, "/api/users/hr/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/users/internal").hasRole("HR")
                        .pathMatchers(HttpMethod.POST, "/api/users/staff").hasRole("HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/users/staff/**").hasRole("HR")
                        .pathMatchers(HttpMethod.POST, "/api/users/guardian").hasRole("RECEPTIONIST")
                        .pathMatchers(HttpMethod.PATCH, "/api/users/guardian/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/users/me/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "RECEPTIONIST",
                                "LAB_AGENT"
                        )
                        .pathMatchers(HttpMethod.PATCH, "/api/users/me/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "RECEPTIONIST",
                                "LAB_AGENT"
                        )
                        .pathMatchers(HttpMethod.POST, "/api/users/me/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "RECEPTIONIST",
                                "LAB_AGENT"
                        )
                        .pathMatchers(HttpMethod.GET, "/api/users/audit").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/users/*/audit").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/users/*/soft-delete").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/users/*/restore").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.GET, "/api/users/guardians").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.POST, "/api/contracts/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PUT, "/api/contracts/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/contracts/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.DELETE, "/api/contracts/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.GET, "/api/contracts").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "RECEPTIONIST",
                                "LAB_AGENT",
                                "GUARDIAN"
                        )
                        .pathMatchers(HttpMethod.GET, "/api/contracts/alerts/action-required").hasAnyRole("ADMIN", "HR", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/contracts/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.GET, "/api/observability/contracts/timeline").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/observability/**").hasAnyRole("ADMIN", "HR", "RECEPTIONIST", "DOCTOR", "NURSE", "SURGEON", "PHARMACIST", "LAB_AGENT", "GUARDIAN")
                        .pathMatchers(HttpMethod.PATCH, "/api/observability/**").hasAnyRole("ADMIN", "HR", "RECEPTIONIST", "DOCTOR", "NURSE", "SURGEON", "PHARMACIST", "LAB_AGENT", "GUARDIAN")
                        .pathMatchers(HttpMethod.POST, "/api/hospital-structure/floors/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hospital-structure/floors/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hospital-structure/floors/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/hospital-structure/workspaces/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hospital-structure/workspaces/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hospital-structure/workspaces/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/hospital-structure/workspaces/*/capacity").hasRole("HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/hospital-structure/workspaces/*/resources/manual").hasRole("HR")
                        .pathMatchers(HttpMethod.GET, "/api/hospital-structure/workspaces/*/resources/assignable").hasRole("HR")
                        .pathMatchers(HttpMethod.POST, "/api/hospital-structure/workspaces/*/assignments").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/hospital-structure/assignments/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.POST, "/api/hospital-resources/**").hasRole("HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/hospital-resources/**").hasRole("HR")
                        .pathMatchers(HttpMethod.PUT, "/api/hospital-resources/**").hasRole("HR")
                        .pathMatchers(HttpMethod.DELETE, "/api/hospital-resources/**").hasRole("HR")
                        .pathMatchers(HttpMethod.GET, "/api/hospital-resources/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.POST, "/api/staff-assignments/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.PATCH, "/api/staff-assignments/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.DELETE, "/api/staff-assignments/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.GET, "/api/staff-assignments/**").hasAnyRole("ADMIN", "HR")
                        .pathMatchers(HttpMethod.GET, "/api/hospital-structure/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "RECEPTIONIST",
                                "LAB_AGENT"
                        )
                        .pathMatchers(HttpMethod.POST, "/api/patients/**").hasRole("RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/patients/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "RECEPTIONIST",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "LAB_AGENT",
                                "GUARDIAN"
                        ).pathMatchers(HttpMethod.GET, "/api/pharmacy/**").hasAnyRole(
                                "ADMIN",
                                "HR",
                                "RECEPTIONIST",
                                "DOCTOR",
                                "NURSE",
                                "SURGEON",
                                "PHARMACIST",
                                "GUARDIAN"
                        )
                        .pathMatchers(HttpMethod.POST, "/api/ai/**").hasAnyRole(
                                "PHARMACIST"
                        )
                        .pathMatchers(HttpMethod.GET, "/api/ai/health").permitAll()

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                ))
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
