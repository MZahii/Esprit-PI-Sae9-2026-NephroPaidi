package tn.esprit.spring.opsservice.hospitalization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignHospitalizationLocationRequest(
        @NotBlank @Size(max = 64) String roomNumber,
        @NotBlank @Size(max = 64) String bedNumber
) {
}
