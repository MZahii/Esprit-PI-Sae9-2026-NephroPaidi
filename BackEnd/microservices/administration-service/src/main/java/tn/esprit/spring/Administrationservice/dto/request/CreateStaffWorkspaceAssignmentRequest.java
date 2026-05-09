package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;

@Getter
@Setter
public class CreateStaffWorkspaceAssignmentRequest {

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @NotNull(message = "role is required")
    private StaffPlacementRole role;

    @NotNull(message = "workspaceId is required")
    @Positive(message = "workspaceId must be positive")
    private Long workspaceId;
}
