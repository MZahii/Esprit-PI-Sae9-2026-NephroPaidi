package tn.esprit.spring.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationEmailRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier;
}

