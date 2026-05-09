package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDTO {
    private Long batchId;
    private String batchNumber;
    private LocalDate manufactureDate;
    private LocalDate expirationDate;
    private Integer quantity;
    private Long medicationId;
    private boolean expired;
}
