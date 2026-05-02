package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;

@Getter
@Builder
public class WorkspaceTypeOptionResponse {
    private WorkspaceType value;
    private String label;
    private String codePrefix;
}
