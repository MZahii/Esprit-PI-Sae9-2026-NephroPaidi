package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HospitalStructureResponse {
    private boolean initialized;
    private int totalFloors;
    private int totalWorkspaces;
    private List<FloorResponse> floors;
}
