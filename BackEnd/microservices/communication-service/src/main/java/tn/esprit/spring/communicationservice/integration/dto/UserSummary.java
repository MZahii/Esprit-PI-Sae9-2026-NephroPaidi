package tn.esprit.spring.communicationservice.integration.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummary {
    private Long id;
    private String keycloakId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean enabled;
    private String avatarUrl;

    public String getDisplayName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        String fullName = (first + " " + last).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return keycloakId;
    }
}
