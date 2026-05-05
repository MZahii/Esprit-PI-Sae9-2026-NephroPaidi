package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceEquipmentRequest {
    @NotNull(message = "equipmentId is required")
    @Positive(message = "equipmentId must be positive")
    private Long equipmentId;
}
