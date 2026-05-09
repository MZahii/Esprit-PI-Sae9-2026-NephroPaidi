package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlacementFloorResponse {
    private Long floorId;
    private String floorLabel;
    private int eligibleWorkspaceCount;
    private int totalPlacedEquipment;
    private List<PlacementWorkspaceSummaryResponse> workspaces;
}
