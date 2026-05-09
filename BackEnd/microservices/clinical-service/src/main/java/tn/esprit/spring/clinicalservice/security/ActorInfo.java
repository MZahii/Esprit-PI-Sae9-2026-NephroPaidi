package tn.esprit.spring.clinicalservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActorInfo {

    private final String id;
    private final String username;
    private final String role;

    public String displayName() {
        if (username != null && !username.isBlank()) {
            if (id != null && !id.isBlank()) {
                return username + " (" + id + ")";
            }
            return username;
        }
        return id;
    }
}
