package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;

@Getter
@Setter
public class CreateEquipmentRequest {
    @NotNull(message = "category is required")
    private EquipmentCategory category;

    private String subtype;

    private String customSubtype;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    @Max(value = 500, message = "quantity cannot exceed 500")
    private Integer quantity;

    private String description;
}
