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
public class CreateWorkspaceRequest {

    @NotNull(message = "floorId is required")
    @Positive(message = "floorId must be positive")
    private Long floorId;

    @NotNull(message = "workspaceType is required")
    private WorkspaceType workspaceType;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 200, message = "quantity cannot exceed 200")
    private Integer quantity;

    private String description;

    private String metadata;
}
