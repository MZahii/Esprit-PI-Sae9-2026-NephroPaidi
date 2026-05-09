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
public class TransferStockRequestDTO {
    private Long sourceBatchId;
    private Long targetBatchId;
    private Integer quantity;
    private String reason;
}
