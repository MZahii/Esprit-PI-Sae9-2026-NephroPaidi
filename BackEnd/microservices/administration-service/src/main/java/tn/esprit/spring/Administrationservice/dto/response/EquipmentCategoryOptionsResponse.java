package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EquipmentCategoryOptionsResponse {
    private String category;
    private List<EquipmentSubtypeOptionResponse> subtypeOptions;
}
