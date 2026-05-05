package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EquipmentInventorySummaryResponse {
    private long total;
    private long available;
    private long underMaintenance;
    private long outOfService;
    private long archived;
    private List<EquipmentCategorySummaryResponse> byCategory;
}
