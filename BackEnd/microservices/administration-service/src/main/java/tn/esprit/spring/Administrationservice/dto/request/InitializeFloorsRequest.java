package tn.esprit.spring.Administrationservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitializeFloorsRequest {

    @NotNull(message = "totalFloors is required")
    @Min(value = 1, message = "At least 1 floor is required")
    @Max(value = 120, message = "totalFloors cannot exceed 120")
    private Integer totalFloors;
}
