package tn.esprit.spring.Administrationservice.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalUserSummary {
    private Long id;
    private String username;
    private String role;
    private String accountStatus;
    private Boolean enabled;
    private Boolean activationPending;
}
