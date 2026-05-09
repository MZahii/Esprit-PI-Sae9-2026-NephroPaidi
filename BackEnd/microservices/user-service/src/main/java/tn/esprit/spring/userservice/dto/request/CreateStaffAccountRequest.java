package tn.esprit.spring.userservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.Sex;

import java.time.LocalDate;

@Getter
@Setter
public class CreateStaffAccountRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "CIN is required")
    private String cin;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotNull(message = "Sex is required")
    private Sex sex;

    @NotNull(message = "Role is required")
    private Role role;

    private String avatarUrl;
}