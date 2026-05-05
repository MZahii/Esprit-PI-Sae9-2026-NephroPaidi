package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.FloorWorkspace;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;

import java.time.LocalDateTime;

@Getter
@Builder
public class WorkspaceResponse {
    private Long id;
    private Long floorId;
    private String floorLabel;
    private Integer floorOrder;
    private WorkspaceType workspaceType;
    private String workspaceTypeLabel;
    private Integer sequenceNumber;
    private String workspaceCode;
    private String workspaceName;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkspaceResponse from(FloorWorkspace workspace, String floorLabel, Integer floorOrder) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .floorId(workspace.getFloor().getId())
                .floorLabel(floorLabel)
                .floorOrder(floorOrder)
                .workspaceType(workspace.getWorkspaceType())
                .workspaceTypeLabel(workspace.getWorkspaceType().getDisplayName())
                .sequenceNumber(workspace.getSequenceNumber())
                .workspaceCode(workspace.getWorkspaceCode())
                .workspaceName(workspace.getWorkspaceName())
                .description(workspace.getDescription())
                .metadata(workspace.getMetadata())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}
