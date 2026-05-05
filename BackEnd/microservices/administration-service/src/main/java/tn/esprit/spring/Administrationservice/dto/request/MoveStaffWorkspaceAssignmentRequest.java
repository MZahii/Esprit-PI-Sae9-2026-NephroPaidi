package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveStaffWorkspaceAssignmentRequest {

    @NotNull(message = "workspaceId is required")
    @Positive(message = "workspaceId must be positive")
    private Long workspaceId;
}
