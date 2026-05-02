package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmartDispenseResponseDTO {
    private Long   medicationId;
    private String medicationName;
    private Integer requested;
    private Integer totalDispensed;
    private List<BatchDispenseLineDTO> lines;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BatchDispenseLineDTO {
        private Long   batchId;
        private String batchNumber;
        private Integer quantityDispensed;
        private String  expirationDate;
    }
}
