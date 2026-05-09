package tn.esprit.spring.pharmacyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferStockResponseDTO {
    private Long sourceBatchId;
    private Long targetBatchId;
    private Integer quantityTransferred;
    private Integer sourceQuantityAvailable;
    private Integer targetQuantityAvailable;
    private String reason;
}
