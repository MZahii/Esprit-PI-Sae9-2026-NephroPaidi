package tn.esprit.spring.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 60, message = "First name must be between 2 and 60 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 60, message = "Last name must be between 2 and 60 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @Pattern(
            regexp = "^$|^[+]?[0-9]{8,15}$",
            message = "Phone must contain 8 to 15 digits, optional leading +"
    )
    private String phone;

    private String avatarUrl;
}
