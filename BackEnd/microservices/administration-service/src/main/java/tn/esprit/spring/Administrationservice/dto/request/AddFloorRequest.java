package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.FloorInsertPosition;

@Getter
@Setter
public class AddFloorRequest {

    @NotNull(message = "position is required")
    private FloorInsertPosition position;

    @Positive(message = "afterFloorId must be positive")
    private Long afterFloorId;

    private String description;
}
