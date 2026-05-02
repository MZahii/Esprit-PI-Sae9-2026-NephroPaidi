package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;
import tn.esprit.spring.Administrationservice.entity.StaffWorkspaceAssignment;

import java.time.LocalDateTime;

@Getter
@Builder
public class StaffWorkspaceAssignmentResponse {
    private Long id;
    private Long userId;
    private StaffPlacementRole role;
    private Long workspaceId;
    private String workspaceCode;
    private String workspaceName;
    private String workspaceType;
    private String floorLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffWorkspaceAssignmentResponse from(StaffWorkspaceAssignment assignment, String floorLabel) {
        return StaffWorkspaceAssignmentResponse.builder()
                .id(assignment.getId())
                .userId(assignment.getUserId())
                .role(assignment.getRole())
                .workspaceId(assignment.getWorkspace().getId())
                .workspaceCode(assignment.getWorkspace().getWorkspaceCode())
                .workspaceName(assignment.getWorkspace().getWorkspaceName())
                .workspaceType(assignment.getWorkspace().getWorkspaceType().name())
                .floorLabel(floorLabel)
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
