package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StaffDirectoryItemResponse {
    private String keycloakId;
    private String username;
}
