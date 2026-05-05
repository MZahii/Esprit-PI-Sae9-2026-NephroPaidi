package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EquipmentCategorySummaryResponse {
    private String category;
    private long count;
}
