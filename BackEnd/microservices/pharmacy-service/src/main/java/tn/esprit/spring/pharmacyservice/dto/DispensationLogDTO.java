package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DispensationLogDTO {
    private Long          id;
    private Long          batchId;
    private String        batchNumber;
    private String        medicationName;
    private Integer       quantity;
    private Long          patientId;
    private String        patientName;
    private Long          prescriptionId;
    private String        dispensedBy;
    private LocalDateTime dispensedAt;
}
