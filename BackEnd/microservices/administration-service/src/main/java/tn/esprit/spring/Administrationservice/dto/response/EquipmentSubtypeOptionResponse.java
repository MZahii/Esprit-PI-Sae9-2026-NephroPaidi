package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EquipmentSubtypeOptionResponse {
    private String value;
    private String label;
}
