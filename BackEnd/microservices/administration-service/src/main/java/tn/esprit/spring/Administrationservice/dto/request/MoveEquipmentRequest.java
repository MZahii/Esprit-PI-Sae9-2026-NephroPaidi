package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveEquipmentRequest {
    @NotNull(message = "targetWorkspaceId is required")
    @Positive(message = "targetWorkspaceId must be positive")
    private Long targetWorkspaceId;
}
