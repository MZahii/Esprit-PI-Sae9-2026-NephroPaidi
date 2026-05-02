package tn.esprit.spring.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    private String role;
    private String redirectTo;

    private Long userId;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean mustChangePassword;
}
