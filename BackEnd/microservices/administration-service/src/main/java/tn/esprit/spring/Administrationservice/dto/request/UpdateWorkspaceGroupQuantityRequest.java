package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;

@Getter
@Setter
public class UpdateWorkspaceGroupQuantityRequest {

    @NotNull(message = "floorId is required")
    @Positive(message = "floorId must be positive")
    private Long floorId;

    @NotNull(message = "workspaceType is required")
    private WorkspaceType workspaceType;

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity cannot be negative")
    @Max(value = 500, message = "quantity cannot exceed 500")
    private Integer quantity;
}
