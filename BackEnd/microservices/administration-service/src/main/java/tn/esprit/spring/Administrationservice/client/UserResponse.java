package tn.esprit.spring.Administrationservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String keycloakId;
    private String username;
    private String cin;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String sex;
    private String role;
    private String accountStatus;
    private boolean enabled;
    private boolean activationPending;
    private boolean statusUpdatePending;
}
