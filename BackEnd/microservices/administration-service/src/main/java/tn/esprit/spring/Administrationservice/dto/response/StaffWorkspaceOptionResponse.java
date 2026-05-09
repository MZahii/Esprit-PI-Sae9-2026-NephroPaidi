package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffWorkspaceOptionResponse {
    private Long workspaceId;
    private String workspaceCode;
    private String workspaceName;
    private String workspaceType;
    private String floorLabel;
    private long assignedCount;
}
