package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.EquipmentStatus;

@Getter
@Setter
public class UpdateEquipmentStatusRequest {
    @NotNull(message = "status is required")
    private EquipmentStatus status;
}
