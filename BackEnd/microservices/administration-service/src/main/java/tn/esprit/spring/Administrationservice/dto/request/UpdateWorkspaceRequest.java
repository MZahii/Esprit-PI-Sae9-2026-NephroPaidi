package tn.esprit.spring.Administrationservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkspaceRequest {
    private String description;
    private String metadata;
}
