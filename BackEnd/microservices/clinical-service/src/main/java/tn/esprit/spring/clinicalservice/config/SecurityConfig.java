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
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

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
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/actuator/health")).permitAll()
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/actuator/info")).permitAll()
                .requestMatchers(mvcMatcherBuilder.pattern("/swagger-ui/**")).permitAll()
                .requestMatchers(mvcMatcherBuilder.pattern("/v3/api-docs/**")).permitAll()
                .requestMatchers(mvcMatcherBuilder.pattern("/clinical/audit/**")).hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                .requestMatchers(mvcMatcherBuilder.pattern("/api/clinical/audit/**")).hasAnyRole("ADMIN", "PLATFORM_ADMIN")
                .requestMatchers(mvcMatcherBuilder.pattern("/clinical/guardian/**")).hasRole("GUARDIAN")
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/clinical/appointments/**")).hasAnyRole("DOCTOR", "RECEPTIONIST", "GUARDIAN")
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, "/clinical/appointments/{id}/start-consultation")).hasRole("DOCTOR")
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, "/clinical/appointments")).hasAnyRole("RECEPTIONIST", "DOCTOR")
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.PUT, "/clinical/appointments/{id}")).hasRole("RECEPTIONIST")
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, "/clinical/appointments/{id}/cancel")).hasRole("RECEPTIONIST")
                .requestMatchers(mvcMatcherBuilder.pattern("/clinical/consultations/**")).hasRole("DOCTOR")
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
