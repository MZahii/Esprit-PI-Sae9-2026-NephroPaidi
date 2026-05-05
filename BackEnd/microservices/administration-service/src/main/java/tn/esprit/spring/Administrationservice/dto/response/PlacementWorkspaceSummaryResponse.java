package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlacementWorkspaceSummaryResponse {
    private Long workspaceId;
    private String workspaceCode;
    private String workspaceName;
    private String workspaceType;
    private int placedCount;
}
