package tn.esprit.spring.communicationservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EscalateRequest {
    private String doctorKeycloakId;
    private String reason;
}
