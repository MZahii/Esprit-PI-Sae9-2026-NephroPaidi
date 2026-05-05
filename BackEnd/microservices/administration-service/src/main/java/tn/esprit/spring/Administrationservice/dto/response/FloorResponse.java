package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FloorResponse {
    private Long id;
    private Integer floorOrder;
    private String floorLabel;
    private String description;
    private Integer totalWorkspaces;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkspaceResponse> workspaces;
}
