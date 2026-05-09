package tn.esprit.spring.userservice.config;

import lombok.Data;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Data
public class KeycloakAdminConfig {

    private String serverUrl;
    private String realm;
    private Admin admin = new Admin();
    private Auth auth = new Auth();
    private Verification verification = new Verification();

    @Data
    public static class Admin {
        private String clientId;
        private String username;
        private String password;
    }

    @Data
    public static class Auth {
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class Verification {
        private boolean enabled = true;
    }

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                    .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(admin.clientId)
                .username(admin.username)
                .password(admin.password)
                .build();
    }
}
