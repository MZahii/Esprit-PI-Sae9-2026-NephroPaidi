package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;
import tn.esprit.spring.Administrationservice.entity.EquipmentStatus;

@Getter
@Setter
public class UpdateEquipmentRequest {
    private String equipmentCode;
    private Boolean autoGenerateCode;

    @NotNull(message = "category is required")
    private EquipmentCategory category;
    private String subtype;

    private String customSubtype;

    @NotNull(message = "status is required")
    private EquipmentStatus status;

    private String description;
}
