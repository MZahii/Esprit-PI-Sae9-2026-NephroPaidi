package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlacementWorkspaceDetailsResponse {
    private Long workspaceId;
    private String workspaceName;
    private String workspaceCode;
    private String workspaceType;
    private String floorLabel;
    private List<String> allowedCategories;
    private List<EquipmentResponse> placedEquipment;
    private List<EquipmentResponse> availableCompatibleEquipment;
}
